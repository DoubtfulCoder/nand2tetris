import java.util.ArrayList;
import java.lang.NumberFormatException;

/**
 * Compilation engine for Jack Compiler
 * Looks through tokens and produces VM output
 * Works with SymbolTable and VMWriter
 */ 

class CompilationEngine {
	String [] inputFile;
	String currentLine;
	int currentLineNum = 0;
	VMWriter outputVMFile = new VMWriter();
	SymbolTable symTable = new SymbolTable(500);
	String className;
	String funcName;
	int numArgsCounter = 0; // counter for args in function call
	int labelCounter = 0; // counter for generating unique labels
	boolean constructorOn = false;
	boolean methodOn = false;

	public CompilationEngine(String[] input) {
		inputFile = input;
		currentLine = inputFile[currentLineNum];
	}

	public static void main(String[] args) {}

	public void compileClass() {
		advance(1);
		className = extractLine();
		advance(2);
		while (extractLine().equals("static") || extractLine().equals("field")) {
			compileClassVarDec();
		}
		while (!extractLine().equals("}")) {
			// Compiles subroutines while end of class not reached
			compileSubroutineDec(); 
		}
	}

	public void compileClassVarDec() {
		String kind = extractLine();
		advance(1);
		String type = extractLine();
		advance(1);
		String line = extractLine();
		while (!line.equals(";")) {
			String varName = line;
			symTable.define(varName, type, kind); // Adds variable to symbol table
			advance(1);
			if (extractLine().equals(",")) {
				advance(1);
			}
			line = extractLine();
		}
		advance(1);
	}

	public void compileSubroutineDec() {
		constructorOn = false;
		methodOn = false;
		String line = extractLine();
		if (line.equals("method")) {
			symTable.define("this", className, "arg"); // first value is this
			methodOn = true; // extra code for method to come
			// outputVMFile.writePush("argument", 0);
		} else if (line.equals("constructor")) {
			constructorOn = true; // extra code for constructor to come
		}

		advance(2);
		funcName = className + '.' + extractLine();
		advance(2);
		compileParameterList();
		// nLocals = symTable.varCount("var")
		compileSubroutineBody();
		symTable.startSubroutine(); // clears symbol table
		advance(1);
	}

	public void compileParameterList() {
		while (!extractLine().equals(")")) {
			String argType = extractLine(); // argument type
			advance(1);
			String argName = extractLine(); // argument name
			symTable.define(argName, argType, "arg"); // adds argument to symbol table
			advance(1);
			if (extractLine().equals(",")) {
				advance(1); // moves extra token if more arguments left
			}
		}
		advance(1);
	}

	public void compileSubroutineBody() {
		advance(1);
		compileVarDec();
		int numLocals = symTable.varCount("var"); // num of local vars for func
		outputVMFile.writeFunction(funcName, numLocals);
		if (constructorOn) {
			int nArgs = symTable.varCount("field"); // amount of space to allocate for object
			outputVMFile.writePush("constant", nArgs);
			outputVMFile.writeCall("Memory.alloc", 1);
			outputVMFile.writePop("pointer", 0); // setting base address of object
		} else if (methodOn) {
			outputVMFile.writePush("argument", 0);
			outputVMFile.writePop("pointer", 0); // setting base address of this
		}
		compileStatements();
	}

	public void compileVarDec() {
		String line = extractLine();
		if (line.equals("var")) {
			advance(1);
			String varType = extractLine();
			while (!extractLine().equals(";")) {
				// Keeps adding vars until semicolon reached
				advance(1);
				String varName = extractLine();
				symTable.define(varName, varType, "var");
				advance(1);
			}
			advance(1);
			compileVarDec();
		}
	}

	public void compileStatements() {
		String line = extractLine();
		// Assumes line is a statement unless end of function reached
		if (!line.equals("}")) {
			advance(1); // moves past keyword
			if (line.equals("let")) {
				compileLet();
			} else if (line.equals("if")) {
				compileIf();
			} else if (line.equals("while")) {
				compileWhile();
			} else if (line.equals("do")) {
				compileDo();
			} else if (line.equals("return")) {
				compileReturn();
			}
			compileStatements();
		}
	}

	public void compileLet() {
		// Gets var from symbol table and evaluates expression
		// Pops expression onto var
		String varName = extractLine();
		advance(1);
		if (extractLine().equals("[")) {
			// Accessing array index
			writeVar(varName, "push"); // base address
			advance(1);
			compileExpression(); // array index
			outputVMFile.writeArithmetic("add");
			advance(2);
			compileExpression();
			outputVMFile.writePop("temp", 0); // temporarily storing value
			outputVMFile.writePop("pointer", 1);
			outputVMFile.writePush("temp", 0); // regenerating value
			outputVMFile.writePop("that", 0);
		} else {
			advance(1);
			compileExpression(); // compiles value of variable 
			writeVar(varName, "pop"); // pops value onto var
		}
	}

	public void compileIf() {
		advance(1);
		String label1 = "label" + labelCounter;
		labelCounter++;

		compileExpression(); // boolean expression of if-statement
		outputVMFile.writeArithmetic("not"); // negating expression for ease of use
		outputVMFile.writeIf(label1); // goes to else/end clause if original expression is false
		advance(2);
		compileStatements(); // body of if
		advance(1);

		// Checks for else clause
		if (extractLine().equals("else")) {
			String label2 = "label" + labelCounter;
			labelCounter++;	
			outputVMFile.writeGoto(label2); // goes to end

			// Start of else
			outputVMFile.writeLabel(label1);
			advance(2);
			compileStatements(); // body of else
			advance(1);

			outputVMFile.writeLabel(label2); // end of if-else clause
		} else {
			outputVMFile.writeLabel(label1); // end
		}
	}

	public void compileWhile() {
		advance(1);
		String label1 = "label" + labelCounter;
		labelCounter++;
		String label2 = "label" + labelCounter;
		labelCounter++;

		outputVMFile.writeLabel(label1);
		compileExpression(); // boolean expression of while 
		outputVMFile.writeArithmetic("not"); 
		outputVMFile.writeIf(label2); // breaks out of while loop once exp is false

		advance(2);
		compileStatements(); // body of while
		advance(1);
		outputVMFile.writeGoto(label1); // next iteration
		outputVMFile.writeLabel(label2); // end of loop
	}

	public void compileDo() {
		String funcName = extractLine();
		advance(1);
		String line = extractLine();

		// Checks if subroutine is from var/class
		if (line.equals(".")) {
			String kind = symTable.kindOf(funcName); // checks if identifier is var or class name
			if (kind.equals("NONE")) {
				// Class name
				funcName += line;
				advance(1);
				funcName += extractLine();
				advance(1);
			} else {	
				// Var name (method being called on object)
				writeVar(funcName, "push");
				numArgsCounter++; // var name is implicit argument to method
				String type = symTable.typeOf(funcName); // class of object
				advance(1);
				funcName = type + '.' + extractLine();
				advance(1);
			}
		} else {
			outputVMFile.writePush("pointer", 0); // this value
			numArgsCounter++;
			funcName = className + '.' + funcName;
		}

		compileExpressionList(); // arguments of call
		advance(1);
		outputVMFile.writeCall(funcName, numArgsCounter);
		outputVMFile.writePop("temp", 0); // return value is useless
		numArgsCounter = 0; // resets counter
		advance(1); // semicolon
	}

	public void compileReturn() {
		if (extractLine().equals(";")) {
			// void subroutine
			outputVMFile.writePush("constant", 0); // dummy value to return
		} else {
			// non-void subroutine
			compileExpression();
		}
		outputVMFile.writeReturn(); // returns top value on stack
		advance(1);
	}

	public void compileExpression() {
		compileTerm();
		String line = extractLine(); // potential operation symbol
		// Checks if expression continues with operation
		while (isOp(convertOp(line))) {
			advance(1);
			compileTerm();
			writeOp(convertOp(line));
			line = extractLine();
		}
	}

	public void compileTerm() {
		String line = extractLine();
		if (currentLine.startsWith("<integerConstant>")) {
			outputVMFile.writePush("constant", convertInt(line));
			advance(1);
		} else if (currentLine.startsWith("<stringConstant>")) {
			// Call OS method for Strings
			int len = line.length();
			outputVMFile.writePush("constant", len);
			outputVMFile.writeCall("String.new", 1); // String creation
			for (int i = 0; i < len; i++) {
				// String assignment one char at a time
				int char_val = (int) line.charAt(i);
				outputVMFile.writePush("constant", char_val);
				outputVMFile.writeCall("String.appendChar", 2);
			}
			advance(1);
		} else if (currentLine.startsWith("<keyword>")) {
			// Keyword constant: true, false, null, this
			writeKeywordConst(line);
			advance(1);
		} else if (convertOp(line) == '-' || convertOp(line) == '~') {
			// Unary operation: - or ~
			String operation = line.equals("-") ? "neg" : "not";
			advance(1);
			compileTerm();
			outputVMFile.writeArithmetic(operation);
		} else if (line.equals("(")) {
			// Expression inside parenthesis
			advance(1);
			compileExpression();
			advance(1);
		} else {
			// Function must look one token to see varName's purpose
			String peakLine = peak();
			switch(peakLine) {
				case("["):
					// Array indexing case
					writeVar(line, "push");
					advance(2);
					compileExpression();
					outputVMFile.writeArithmetic("add");
					outputVMFile.writePop("pointer", 1);
					outputVMFile.writePush("that", 0);
					break;
				case("("):
					// Subroutine call
					String funcName = line;
					funcName = className + '.' + funcName;
					advance(1);
					compileExpressionList();
					outputVMFile.writeCall(funcName, numArgsCounter);
					numArgsCounter = 0;
					break;
				case("."):
					// varName.subroutineCall or className.subroutineCall
					String subroutineName = "";
					String kind = symTable.kindOf(line); // checks if identifier is var or class name
					if (kind.equals("NONE")) {
						// Class name
						subroutineName = line + peakLine;
						advance(2);
						subroutineName += extractLine();
					} else {	
						// Var name (method being called on object)
						writeVar(line, "push");
						numArgsCounter++; // var name is implicit argument to method
						String type = symTable.typeOf(line); // class of object
						advance(2);
						subroutineName = type + '.' + extractLine();
					}
					advance(1);
					compileExpressionList();
					outputVMFile.writeCall(subroutineName, numArgsCounter);
					numArgsCounter = 0;
					break;
				default:
					// Normal variable
					writeVar(line, "push");
					break;
			}
			advance(1);
		}
	}

	public void compileExpressionList() {
		advance(1);
		while (!extractLine().equals(")")) {
			compileExpression();
			numArgsCounter++;
			// Checks if more expressions left in list
			if (extractLine().equals(",")) {
				advance(1); // moves past comma
			}
		}	
	}

	public String advance(int n) {
		// Advances n lines and returns line
		currentLineNum += n;
		currentLine = inputFile[currentLineNum];
		return currentLine;
	}

	public String extractLine() {
		// Extracts line from XML tags
		String line = currentLine.replaceAll("\\s?<.*?>\\s?", ""); // Removes open and close tag
		return line;
	}

	public String peak() {
		// Peaks at next line
		String line = inputFile[currentLineNum + 1];
		line = line.replaceAll("\\s*<.*?>\\s*", "");
		return line;
	}

	public void writeOp(char op) {
		// Converts operation symbol to VM operation command
		// Adds converted symbol to VM output file
		switch(op) {
			case('+'):
				outputVMFile.writeArithmetic("add");
				break;
			case('-'):
				outputVMFile.writeArithmetic("sub");
				break;
			case('&'):
				outputVMFile.writeArithmetic("and");
				break;
			case('|'):
				outputVMFile.writeArithmetic("or");
				break;
			case('<'):
				outputVMFile.writeArithmetic("lt");
				break;
			case('>'):
				outputVMFile.writeArithmetic("gt");
				break;
			case('='):
				outputVMFile.writeArithmetic("eq");
				break;
			case('*'):
				outputVMFile.writeCall("Math.multiply", 2);
				break;
			case('/'):
				outputVMFile.writeCall("Math.divide", 2);
				break;
		}
	}

	public void writeKeywordConst(String keyword) {
		// Writes VM code for keyword constants
		switch(keyword) {
			case("true"):
				outputVMFile.writePush("constant", 1);
				outputVMFile.writeArithmetic("neg"); // true value
				break;
			case("false"):
			case("null"):
				outputVMFile.writePush("constant", 0); // false/null value
				break;
			case("this"):
				// }
				outputVMFile.writePush("pointer", 0);
				break;
		}
	}

	public void writeVar(String varName, String pushOrPop) {
		// Writes push/pop command for getting/setting var
		// Attempts to search subroutine table and then class table
		String[] varInfo = symTable.subroutineTable.get(varName);
        if (varInfo == null) {
        	varInfo = symTable.classTable.get(varName);
        }

        String kind = varInfo[0];
        int index = convertInt(varInfo[2]);
        if (kind.equals("field")) {
        	kind = "this";
        } else if (kind.equals("var")) {
        	kind = "local";
        } else if (kind.equals("arg")) {
        	kind = "argument";
        }

		if (pushOrPop.equals("push")) {
			outputVMFile.writePush(kind, index);
		} else if (pushOrPop.equals("pop")) {
			outputVMFile.writePop(kind, index);
		}
	}

	public static int convertInt(String num) {
		// Converts string to int and assumes "error-free"
		try {
			return Integer.parseInt(num);
		} catch(NumberFormatException e) {
			return 99999999; // random invalid number in jack
		}
	}

	public static char convertOp(String op) {
		// Converts string op to char
		switch(op) {
			case("&amp;"):
				return '&';
			case("&lt;"):
				return '<';
			case("&gt;"):
				return '>';
			case("&eq;"):
				return '=';
			default:
				return op.charAt(0);
		}
	}

	public static boolean isOp(char testChar) {
		// Checks if testChar is a valid operation symbol
		char[] operations = {'+', '-', '*', '/', '&', '|', '<', '>', '='};
		for (char op : operations) {
			if (testChar == op) {
				return true;
			}
		}
		return false;
	}
}