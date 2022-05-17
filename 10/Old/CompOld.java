import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.MatchResult;
import java.util.ArrayList;

/**
 * Receives tokenized file as input
 * Uses jack grammar to produce structured output
 */
class CompilationEngine {
	String[] inputFile;
	String currentLine;
	int currentLineNum = 0;
	String outputFile = "";
	
	public CompilationEngine(String[] input) {
		inputFile = input;
		currentLine = inputFile[currentLineNum];
	}

	public static void main(String[] args) {
		String strFile = "<keyword> class </keyword>\n<identifier> Square </identifier>\n<symbol> { </symbol>\n<keyword> field </keyword>\n<keyword> int </keyword>\n<identifier> x </identifier>\n<symbol> , </symbol>\n<identifier> y </identifier>\n<symbol> ; </symbol>\n<keyword> field </keyword>\n<keyword> int </keyword>\n<identifier> size </identifier>\n<symbol> ; </symbol>\n<keyword> constructor </keyword>\n<identifier> Square </identifier>\n<identifier> new </identifier>\n<symbol> ( </symbol>\n<keyword> int </keyword>\n<identifier> Ax </identifier>\n<symbol> , </symbol>\n<keyword> int </keyword>\n<identifier> Ay </identifier>\n<symbol> , </symbol>\n<keyword> int </keyword>\n<identifier> Asize </identifier>\n<symbol> ) </symbol>\n<symbol> { </symbol>\n<keyword> let </keyword>\n<identifier> x </identifier>\n<symbol> = </symbol>\n<identifier> Ax </identifier>\n<symbol> ; </symbol>\n<keyword> let </keyword>\n<identifier> y </identifier>\n<symbol> = </symbol>\n<identifier> Ay </identifier>\n<symbol> ; </symbol>\n<keyword> let </keyword>\n<identifier> size </identifier>\n<symbol> = </symbol>\n<identifier> Asize </identifier>\n<symbol> ; </symbol>\n<keyword> do </keyword>\n<identifier> draw </identifier>\n<symbol> ( </symbol>\n<symbol> ) </symbol>\n<symbol> ; </symbol>\n<keyword> return </keyword>\n<identifier> x </identifier>\n<symbol> ; </symbol>\n<symbol> } </symbol>";
		// String strFile = "<keyword> class </keyword>\n<identifier> SquareGame </identifier>\n<symbol> { </symbol>\n<keyword> field </keyword>\n<identifier> Square </identifier>\n<identifier> square </identifier>\n<symbol> ; </symbol>\n<keyword> field </keyword>\n<keyword> int </keyword>\n<identifier> direction </identifier>\n<symbol> ; </symbol>\n<symbol> } </symbol>";	
		String[] file = strFile.split("\n");
		CompilationEngine compilation = new CompilationEngine(file);
		compilation.compileClass();
	}

	public void compileClass() {
		// <keyword> class </keyword>
		// regex: "class .* \\{ .* \\}"
		outputFile += "<class>";
		outputFile += currentLine; // Class keyword
		outputFile += advanceAndGetLine(); // Class name
		System.out.println(outputFile);
		outputFile += advanceAndGetLine(); // Opening curly brace

		compileClassVarDec();
		compileSubroutineDec();

		outputFile += advanceAndGetLine(); // Closing curly brace
		outputFile += "</class>";
		System.out.println(outputFile);
	}

	public void compileClassVarDec() {
		// Matches either a static or field declaration
		advanceAndGetLine();
		String line = extractToken();
		if (line.equals("static") || line.equals("field")) {
			outputFile += "<classVarDec>";
			outputFile += currentLine; // field/static keyword
			while (!extractToken().equals(";")) {
				outputFile += advanceAndGetLine(); // Adds parts of declaration
			}
			outputFile += "</classVarDec>";
			compileClassVarDec();
		}
	}

	public void compileSubroutineDec() {
		String line = extractToken();
		if (line.equals("constructor") || line.equals("function") 
						|| line.equals("method")) {
			outputFile += "<subroutineDec>";
			outputFile += currentLine; 	// constructor/function/method keyword
			for (int i = 0; i < 3; i++) {
				// Adds type, subroutine name, and left parenthesis
				outputFile += advanceAndGetLine();
			}
			compileParameterList();
			outputFile += currentLine; // closing paren
			compileSubroutineBody();
			outputFile += "</subroutineDec>";
		}
	}

	public void compileParameterList() {
		// Adds type and varName of all params
		outputFile += "<parameterList>";
		advanceAndGetLine();
		while (!extractToken().equals(")")) {
			outputFile += currentLine;
			advanceAndGetLine();
		}
		outputFile += "</parameterList>";
	}

	public void compileSubroutineBody() {
		outputFile += "<subroutineBody>";
		outputFile += advanceAndGetLine(); // Opening curly brace
		compileVarDec(); // Compiles local variables
		compileStatements(); // Compiles any other statements
		outputFile += currentLine; // Closing curly brace
		outputFile += "</subroutineBody>";
	}

	public void compileVarDec() {
		// Matches any local variable declarations
		advanceAndGetLine();
		String line = extractToken();
		if (line.equals("var")) {
			outputFile += "<varDec>";
			outputFile += currentLine; // var keyword
			while (!extractToken().equals(";")) {
				outputFile += advanceAndGetLine(); // Adds parts of declaration
			}
			outputFile += "</varDec>";
			compileVarDec();
		}
	}

	public void compileStatements() {
		outputFile += "<statements>";
		String statement = extractToken();
		while (isStatement(statement)) {
			if (statement.equals("let")) {
				compileLet();
			} else if (statement.equals("if")) {
				compileIf();
			} else if (statement.equals("while")) {
				compileWhile();
			} else if (statement.equals("do")) {
				compileDo();
			} else if (statement.equals("return")) {
				compileReturn();
			}
			advanceAndGetLine();
			statement = extractToken();
		}
		outputFile += "</statements>";
	}

	public void compileLet() {
		outputFile += "<letStatement>";
		outputFile += currentLine; // let keyword 
		outputFile += advanceAndGetLine(); // left paren
		// Potential expression for arrays
		advanceAndGetLine();
		if (extractToken().equals("[")) {
			outputFile += currentLine; // left brace
			compileExpression(); // expression in braces
			outputFile += currentLine; // right brace
		} 
		outputFile += currentLine; // equals sign
		compileExpression();
		outputFile += advanceAndGetLine(); // semicolon
		outputFile += "</letStatement>";
	}

	public void compileIf() {
		outputFile += "<ifStatement>";
		outputFile += currentLine; // if keyword 
		outputFile += advanceAndGetLine(); // left paren
		compileExpression(); // condition of if-statement
		outputFile += currentLine; // right parenthesis
		outputFile += advanceAndGetLine(); // left brace
		compileStatements(); // body of if-statement
		outputFile += currentLine; // right brace

		// Checks for else clause
		if (extractToken().equals("else")) {
			outputFile += advanceAndGetLine(); // else keyword
			outputFile += advanceAndGetLine(); // left brace
			compileStatements(); // body of else
			outputFile += currentLine; // right brace
		}
		outputFile += "</ifStatement>";
	}

	public void compileWhile() {
		outputFile += "<whileStatement>";
		outputFile += currentLine; // while keyword 
		outputFile += advanceAndGetLine(); // left paren
		compileExpression(); // condition of loop
		outputFile += currentLine; // right parenthesis
		outputFile += advanceAndGetLine(); // left brace
		compileStatements(); // body of loop
		outputFile += currentLine; // right brace
		outputFile += "</whileStatement>";
	}

	public void compileDo() {
		outputFile += "<doStatement>";
		outputFile += currentLine; // do keyword
		addNLines(2); // Adds subroutineName/className/varName and left paren/dot

		// Class/variable subroutine name
		advanceAndGetLine();
		if (extractToken().equals(".")) {
			addNLines(2); // subroutineName and left paren
		}
		compileExpressionList(); // arguments of call
		outputFile += currentLine; // right paren
		outputFile += advanceAndGetLine(); // semicolon
		outputFile += "</doStatement>";
	}

	public void compileReturn() {
		outputFile += "<returnStatement>";
		outputFile += currentLine; // return keyword
		if (!extractToken().equals("<symbol> ; </symbol>")) {
			compileExpression(); // expression in return statement
		}
		outputFile += advanceAndGetLine(); // semicolon
		outputFile += "</returnStatement>";
	}

	public void compileExpression() {
		outputFile += "<expression>";
		compileTerm();
		advanceAndGetLine();
		while (isOp(extractToken().charAt(0))) {
			System.out.println(outputFile);
			outputFile += currentLine;
			compileTerm();
		}
		outputFile += "</expression>";
	}

	public void compileTerm() {
		outputFile += "<term>";
		advanceAndGetLine();
		if (currentLine.startsWith("<integerConstant>") 
				|| currentLine.startsWith("<stringConstant>")
				|| currentLine.startsWith("<keyword>")
				|| isUnaryOp(extractToken().charAt(0))) {
			outputFile += currentLine;
		} else {
			// Function must look one token to see varName's purpose
			outputFile += advanceAndGetLine(); // varName
			String peakNextChar = inputFile[currentLineNum + 1];
			if (peakNextChar.equals("<symbol> [ </symbol>")) {
				// Array indexing case
				outputFile += advanceAndGetLine(); // opening bracket
				compileExpression(); // expression in brackets
				outputFile += advanceAndGetLine(); // closing bracket
			} else if (peakNextChar.equals("<symbol> . </symbol>")) {
				// Subroutine call case
				addNLines(3); // dot, subroutineName, and left paren
				compileExpressionList(); // arguments of call
				outputFile += advanceAndGetLine(); // right paren
			} else {
				// Normal var case
				outputFile += advanceAndGetLine(); // varName
			}
		}
		outputFile += "</term>";
	}

	public void compileExpressionList() {
		// Keeps compiling expressions in list until none left
		outputFile += "<expressionList>";
		while (!extractToken().equals(")")) {	
			compileExpression();
			advanceAndGetLine();
			if (extractToken().equals(",")) {
				advanceAndGetLine(); // moves to next expression in list
			}
		}
		outputFile += "</expressionList>";
	}

	public void addNLines(int n) {
		// Adds next n lines to output
		for (; n > 0; n--) {
			outputFile += advanceAndGetLine() + "\n";
		}
	}

	public String advanceAndGetLine() {
		// Advances to next line and then returns line
		currentLineNum++;
		currentLine = inputFile[currentLineNum];
		return currentLine;
	}

	public String extractToken() {
		// Extracts tokens from XML tags
		String line = currentLine.replaceFirst("<.*?>\\s", ""); // Removes opening tag
		line = line.replaceFirst("\\s</.*?>", ""); // Removes closing tag
		return line;
	}

	public boolean isStatement(String statement) {
		// Takes statement word and returns whether it is valid
		if (statement.equals("let") || statement.equals("if") 
			|| statement.equals("while") || statement.equals("do")
			|| statement.equals("return")) {
			return true;
		}
		return false;
	}

	public boolean isUnaryOp(char testChar) {
		if (testChar == '-' || testChar == '~') {
			return true;
		}
		return false;
	}

	public boolean isOp(char testChar) {
		char[] operations = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', 
							'-', '*', '/', '&', '|', '<', '>', '=', '~'};
		for (char op : operations) {
			if (testChar == op) {
				return true;
			}
		}
		return false;
	}

	public String regexFinder(String regex, String text) {
		// Takes in a regex and returns matches
		Pattern pattern = Pattern.compile(regex);
		Matcher matches = pattern.matcher(text);
		matches.find();
		return matches.group();
	}
}