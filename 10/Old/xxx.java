import java.util.ArrayList;

/**
 * Receives tokenized file as input
 * Uses jack grammar to produce structured output
 */
class CompilationEngine {
	String[] inputFile;
	String currentLine;
	int currentLineNum = 0;
	ArrayList<String> outputFile = new ArrayList<String>();
	
	public CompilationEngine(String[] input) {
		inputFile = input;
		currentLine = inputFile[currentLineNum];
	}

	public static void main(String[] args) {}

	public void compileClass() {
		outputFile.add("<class>");
		outputFile.add(currentLine); // Class keyword
		addNLines(2); // Class name and opening curly brace
		// outputFile.add(advanceAndGetLine()); // Class name
		// outputFile.add(advanceAndGetLine()); // Opening curly brace

		compileClassVarDec(); // class variables
		compileSubroutineDec(); // class functions

		outputFile.add(currentLine); // Closing curly brace
		outputFile.add("</class>");
	}

	public void compileClassVarDec() {
		// Matches either a static or field declaration
		advanceAndGetLine();
		String line = extractToken();
		if (line.equals("static") || line.equals("field")) {
			outputFile.add("<classVarDec>");
			outputFile.add(currentLine); // field/static keyword
			while (!extractToken().equals(";")) {
				outputFile.add(advanceAndGetLine()); // Adds parts of declaration
			}
			outputFile.add("</classVarDec>");
			compileClassVarDec();
		}
	}

	public void compileSubroutineDec() {
		String line = extractToken();
		if (line.equals("constructor") || line.equals("function") 
						|| line.equals("method")) {
			outputFile.add("<subroutineDec>");
			outputFile.add(currentLine); 	// constructor/function/method keyword
			for (int i = 0; i < 3; i++) {
				// Adds type, subroutine name, and left parenthesis
				outputFile.add(advanceAndGetLine());
			}
			compileParameterList();
			outputFile.add(currentLine); // closing paren
			compileSubroutineBody();
			outputFile.add("</subroutineDec>");
			advanceAndGetLine();
			compileSubroutineDec(); // goes to next subroutine
		}
	}

	public void compileParameterList() {
		// Adds type and varName of all params
		outputFile.add("<parameterList>");
		advanceAndGetLine();
		while (!extractToken().equals(")")) {
			outputFile.add(currentLine);
			advanceAndGetLine();
		}
		outputFile.add("</parameterList>");
	}

	public void compileSubroutineBody() {
		outputFile.add("<subroutineBody>");
		outputFile.add(advanceAndGetLine()); // Opening curly brace
		compileVarDec(); // Compiles local variables
		compileStatements(); // Compiles main statements
		outputFile.add(currentLine); // Closing curly brace
		outputFile.add("</subroutineBody>");
	}

	public void compileVarDec() {
		// Matches any local variable declarations
		advanceAndGetLine();
		String line = extractToken();
		if (line.equals("var")) {
			outputFile.add("<varDec>");
			outputFile.add(currentLine); // var keyword
			while (!extractToken().equals(";")) {
				outputFile.add(advanceAndGetLine()); // Adds parts of declaration
			}
			outputFile.add("</varDec>");
			compileVarDec();
		}
	}

	public void compileStatements() {
		outputFile.add("<statements>");
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
		outputFile.add("</statements>");
	}

	public void compileLet() {
		outputFile.add("<letStatement>");
		outputFile.add(currentLine); // let keyword 
		outputFile.add(advanceAndGetLine()); // varName
		// Potential expression for arrays
		advanceAndGetLine();
		if (extractToken().equals("[")) {
			outputFile.add(currentLine); // left brace
			compileExpression(); // expression in braces
			outputFile.add(currentLine); // right brace
			advanceAndGetLine();
		} 
		outputFile.add(currentLine); // equals sign
		compileExpression();
		outputFile.add(currentLine); // semicolon
		outputFile.add("</letStatement>");
	}

	public void compileIf() {
		outputFile.add("<ifStatement>");
		compileWhileIf();

		// Checks for else clause
		if (peak().equals("<keyword> else </keyword>")) {
			addNLines(3); // else keyword, left brace, and start of body
			compileStatements(); // body of else
			outputFile.add(currentLine); // right brace
		}
		outputFile.add("</ifStatement>");
	}

	public void compileWhile() {
		outputFile.add("<whileStatement>");
		compileWhileIf();
		outputFile.add("</whileStatement>");
	}

	public void compileWhileIf() {
		// While and if have the same starting commands
		outputFile.add(currentLine); // while/if keyword 
		outputFile.add(advanceAndGetLine()); // left paren
		compileExpression(); // condition of loop/if-statement
		outputFile.add(currentLine); // right parenthesis
		outputFile.add(advanceAndGetLine()); // left brace
		advanceAndGetLine();
		compileStatements(); // body of loop/if-statement
		outputFile.add(currentLine); // right brace
	}

	public void compileDo() {
		outputFile.add("<doStatement>");
		outputFile.add(currentLine); // do keyword
		addNLines(2); // Adds subroutineName/className/varName and left paren/dot

		// Checks for class/variable subroutine name
		if (extractToken().equals(".")) {
			addNLines(2); // subroutineName and left paren
		}
		compileExpressionList(); // arguments of call
		outputFile.add(currentLine); // right paren
		outputFile.add(advanceAndGetLine()); // semicolon
		outputFile.add("</doStatement>");
	}

	public void compileReturn() {
		outputFile.add("<returnStatement>");
		outputFile.add(currentLine); // return keyword
		advanceAndGetLine();
		if (!extractToken().equals(";")) {
			currentLineNum--;
			currentLine = inputFile[currentLineNum]; // setting line back one to offset extract
			compileExpression(); // expression in return statement
		}
		outputFile.add(currentLine); // semicolon
		outputFile.add("</returnStatement>");
	}

	public void compileExpression() {
		outputFile.add("<expression>");
		compileTerm();
		advanceAndGetLine();
		// Compiles terms while more operators left in expression
		while (isOp(extractToken().charAt(0))) {
			outputFile.add(currentLine);
			compileTerm();
			advanceAndGetLine();
		}
		outputFile.add("</expression>");
	}

	public void compileTerm() {
		outputFile.add("<term>");
		advanceAndGetLine();
		if (currentLine.startsWith("<integerConstant>") 
				|| currentLine.startsWith("<stringConstant>")
				|| currentLine.startsWith("<keyword>")) {
			outputFile.add(currentLine);
		} else if (isUnaryOp(extractToken().charAt(0))) {
			// Unary operation: - or ~
			outputFile.add(currentLine); // unary symbol
			compileTerm();
		} else if (extractToken().equals("(")) {
			// Expression inside parenthesis
			outputFile.add(currentLine); // left paren
			compileExpression(); // expression in paren
			outputFile.add(currentLine); // right paren
		} else {
			// Function must look one token to see varName's purpose
			outputFile.add(currentLine); // varName
			String peakNextChar = peak();
			if (peakNextChar.equals("<symbol> [ </symbol>")) {
				// Array indexing case
				outputFile.add(advanceAndGetLine()); // opening bracket
				compileExpression(); // expression in brackets
				outputFile.add(currentLine); // closing bracket
			} else if (peakNextChar.equals("<symbol> . </symbol>")) {
				// Subroutine call case
				addNLines(3); // dot, subroutineName, and left paren
				compileExpressionList(); // arguments of call
				outputFile.add(currentLine); // right paren
			}
		}
		outputFile.add("</term>");
	}

	public void compileExpressionList() {
		// Keeps compiling expressions in list until none left
		outputFile.add("<expressionList>");
		advanceAndGetLine();
		while (!extractToken().equals(")")) {
			currentLineNum--;
			currentLine = inputFile[currentLineNum]; // setting line back one to offset extract	
			compileExpression();
			if (extractToken().equals(",")) {
				outputFile.add(currentLine); // comma symbol
				advanceAndGetLine(); // moves to next expression in list
			}
		}
		outputFile.add("</expressionList>");
	}

	public String peak() {
		// Peaks at next line
		return inputFile[currentLineNum + 1];
	}

	public void addNLines(int n) {
		// Adds next n lines to output
		for (; n > 0; n--) {
			outputFile.add(advanceAndGetLine());
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
		// Checks if word is a valid statement starter
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
		char[] operations = {'+', '-', '*', '/', '&', '|', '<', '>', '='};
		for (char op : operations) {
			if (testChar == op) {
				return true;
			}
		}
		return false;
	}
}



















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
	ArrayList<String> outputFile = new ArrayList<String>();
	
	public CompilationEngine(String[] input) {
		inputFile = input;
		currentLine = inputFile[currentLineNum];
	}

	public static void main(String[] args) {}

	public void compileClass() {
		outputFile.add("<class>");
		outputFile.add(currentLine); // Class keyword
		outputFile.add(advanceAndGetLine()); // Class name
		outputFile.add(advanceAndGetLine()); // Opening curly brace

		compileClassVarDec();
		compileSubroutineDec();

		outputFile.add(currentLine); // Closing curly brace
		outputFile.add("</class>");
	}

	public void compileClassVarDec() {
		// Matches either a static or field declaration
		advanceAndGetLine();
		String line = extractToken();
		if (line.equals("static") || line.equals("field")) {
			outputFile.add("<classVarDec>");
			outputFile.add(currentLine); // field/static keyword
			while (!extractToken().equals(";")) {
				outputFile.add(advanceAndGetLine()); // Adds parts of declaration
			}
			outputFile.add("</classVarDec>");
			compileClassVarDec();
		}
	}

	public void compileSubroutineDec() {
		String line = extractToken();
		if (line.equals("constructor") || line.equals("function") 
						|| line.equals("method")) {
			outputFile.add("<subroutineDec>");
			outputFile.add(currentLine); 	// constructor/function/method keyword
			for (int i = 0; i < 3; i++) {
				// Adds type, subroutine name, and left parenthesis
				outputFile.add(advanceAndGetLine());
			}
			compileParameterList();
			outputFile.add(currentLine); // closing paren
			compileSubroutineBody();
			outputFile.add("</subroutineDec>");
			advanceAndGetLine();
			compileSubroutineDec(); // goes to next subroutine
		}
	}

	public void compileParameterList() {
		// Adds type and varName of all params
		outputFile.add("<parameterList>");
		advanceAndGetLine();
		while (!extractToken().equals(")")) {
			outputFile.add(currentLine);
			advanceAndGetLine();
		}
		outputFile.add("</parameterList>");
	}

	public void compileSubroutineBody() {
		outputFile.add("<subroutineBody>");
		outputFile.add(advanceAndGetLine()); // Opening curly brace
		compileVarDec(); // Compiles local variables
		compileStatements(); // Compiles main statements
		outputFile.add(currentLine); // Closing curly brace
		outputFile.add("</subroutineBody>");
	}

	public void compileVarDec() {
		// Matches any local variable declarations
		advanceAndGetLine();
		String line = extractToken();
		if (line.equals("var")) {
			outputFile.add("<varDec>");
			outputFile.add(currentLine); // var keyword
			while (!extractToken().equals(";")) {
				outputFile.add(advanceAndGetLine()); // Adds parts of declaration
			}
			outputFile.add("</varDec>");
			compileVarDec();
		}
	}

	public void compileStatements() {
		outputFile.add("<statements>");
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
		outputFile.add("</statements>");
	}

	public void compileLet() {
		outputFile.add("<letStatement>");
		outputFile.add(currentLine); // let keyword 
		outputFile.add(advanceAndGetLine()); // varName
		// Potential expression for arrays
		advanceAndGetLine();
		if (extractToken().equals("[")) {
			outputFile.add(currentLine); // left brace
			compileExpression(); // expression in braces
			outputFile.add(currentLine); // right brace
			advanceAndGetLine();
		} 
		outputFile.add(currentLine); // equals sign
		compileExpression();
		outputFile.add(currentLine); // semicolon
		outputFile.add("</letStatement>");
	}

	public void compileIf() {
		outputFile.add("<ifStatement>");
		outputFile.add(currentLine); // if keyword 
		outputFile.add(advanceAndGetLine()); // left paren
		compileExpression(); // condition of if-statement
		outputFile.add(currentLine); // right parenthesis
		outputFile.add(advanceAndGetLine()); // left brace
		advanceAndGetLine();
		compileStatements(); // body of if-statement
		outputFile.add(currentLine); // right brace

		// Checks for else clause
		if (peak().equals("<keyword> else </keyword>")) {
			outputFile.add(advanceAndGetLine()); // else keyword
			outputFile.add(advanceAndGetLine()); // left brace
			advanceAndGetLine();
			compileStatements(); // body of else
			outputFile.add(currentLine); // right brace
		}
		outputFile.add("</ifStatement>");
	}

	public void compileWhile() {
		outputFile.add("<whileStatement>");
		outputFile.add(currentLine); // while keyword 
		outputFile.add(advanceAndGetLine()); // left paren
		compileExpression(); // condition of loop
		outputFile.add(currentLine); // right parenthesis
		outputFile.add(advanceAndGetLine()); // left brace
		advanceAndGetLine();
		compileStatements(); // body of loop
		outputFile.add(currentLine); // right brace
		outputFile.add("</whileStatement>");
	}

	public void compileDo() {
		outputFile.add("<doStatement>");
		outputFile.add(currentLine); // do keyword
		addNLines(2); // Adds subroutineName/className/varName and left paren/dot

		// Class/variable subroutine name
		if (extractToken().equals(".")) {
			addNLines(2); // subroutineName and left paren
		}
		compileExpressionList(); // arguments of call
		outputFile.add(currentLine); // right paren
		outputFile.add(advanceAndGetLine()); // semicolon
		outputFile.add("</doStatement>");
	}

	public void compileReturn() {
		outputFile.add("<returnStatement>");
		outputFile.add(currentLine); // return keyword
		advanceAndGetLine();
		if (!extractToken().equals(";")) {
			currentLineNum--;
			currentLine = inputFile[currentLineNum]; // setting line back one to offset extract
			compileExpression(); // expression in return statement
		}
		outputFile.add(currentLine); // semicolon
		outputFile.add("</returnStatement>");
	}

	public void compileExpression() {
		outputFile.add("<expression>");
		compileTerm();
		advanceAndGetLine();
		while (isOp(extractToken().charAt(0))) {
			outputFile.add(currentLine);
			compileTerm();
			advanceAndGetLine();
		}
		outputFile.add("</expression>");
	}

	public void compileTerm() {
		outputFile.add("<term>");
		advanceAndGetLine();
		if (currentLine.startsWith("<integerConstant>") 
				|| currentLine.startsWith("<stringConstant>")
				|| currentLine.startsWith("<keyword>")) {
			outputFile.add(currentLine);
		} else if (isUnaryOp(extractToken().charAt(0))) {
			// Unary operation: - or ~
			outputFile.add(currentLine); // unary symbol
			compileTerm();
		} else if (extractToken().equals("(")) {
			// Expression inside parenthesis
			outputFile.add(currentLine); // left paren
			compileExpression();
			outputFile.add(currentLine); // right paren
		} else {
			// Function must look one token to see varName's purpose
			outputFile.add(currentLine); // varName
			String peakNextChar = inputFile[currentLineNum + 1];
			if (peakNextChar.equals("<symbol> [ </symbol>")) {
				// Array indexing case
				outputFile.add(advanceAndGetLine()); // opening bracket
				compileExpression(); // expression in brackets
				outputFile.add(currentLine); // closing bracket
			} else if (peakNextChar.equals("<symbol> . </symbol>")) {
				// Subroutine call case
				addNLines(3); // dot, subroutineName, and left paren
				compileExpressionList(); // arguments of call
				outputFile.add(currentLine); // right paren
			}
		}
		outputFile.add("</term>");
	}

	public void compileExpressionList() {
		// Keeps compiling expressions in list until none left
		outputFile.add("<expressionList>");
		advanceAndGetLine();
		while (!extractToken().equals(")")) {
			currentLineNum--;
			currentLine = inputFile[currentLineNum]; // setting line back one to offset extract	
			compileExpression();
			if (extractToken().equals(",")) {
				outputFile.add(currentLine); // comma symbol
				advanceAndGetLine(); // moves to next expression in list
			}
		}
		outputFile.add("</expressionList>");
	}

	public String peak() {
		// Peaks at next line
		return inputFile[currentLineNum + 1];
	}

	public void addNLines(int n) {
		// Adds next n lines to output
		for (; n > 0; n--) {
			outputFile.add(advanceAndGetLine());
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
		char[] operations = {'+', '-', '*', '/', '&', '|', '<', '>', '='};
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




import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.MatchResult;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Creates tokens for Jack file
 * Outputs to Xml file
 */ 
public class Testing {
	public static String[] keywords = {"class", "constructor", "function", "method",
										"field", "static", "var", "int", "char", 
										"boolean", "void", "true", "false", "null", 
										"this", "let", "do", "if", "else", "while", "return"};
	public static char[] operations = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', 
										'-', '*', '/', '&', '|', '<', '>', '=', '~'};
	String[] fileContents;
	String currentToken;
	String currentLine;
	int currentLineNum;
	String XMLFile;

	public Testing(String[] contents) {
		fileContents = contents;
		currentToken = "";
		currentLineNum = 0;
		XMLFile = "";
	}

	public static void main(String[] args) {
		// Opens either a file or directory;
		// If directory, opens all jack files in dir
		String fileDirName = args[0];
		if (fileDirName.indexOf('.') != -1) {
			System.out.println(openFile(fileDirName)); // Opens file
		} else {
			// Opens each file in directory
			File dir = new File(fileDirName);
			File[] filesInDir = dir.listFiles();
			for (File f : filesInDir) {
				String filePath = f.getAbsolutePath();
				if (filePath.endsWith("jack")) {
					// Removes whitespace from file and tokenizes
					String[] file_no_whitespace = whiteSpaceRem(openFile(filePath));
					Testing file = new Testing(file_no_whitespace);
					file.run();
					file.writeToXml(filePath);
				}
			}
		}
	}

	public static String[] openFile(String filename) {
		// Returns content of file
		String fileContents = "";
		try {
			File fileObj = new File(filename);
			Scanner fileRead = new Scanner(fileObj);
			while (fileRead.hasNextLine()) {
				fileContents += fileRead.nextLine() + '\n';
			}
			fileRead.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		}
		return fileContents.split("\n");
	}

	public static String[] whiteSpaceRem(String[] file)	{
		// Removes all whitespace and comments
		String final_file = "";
		for (String line : file) {
			// Removes inline comments
			line = line.replaceFirst("//.*", "");
			// Removes trailing whitespace
			line = line.strip();
			// Only appends non-empty and non-comment lines
			if (!(line.startsWith("/*") || line.length() == 0)) {
				// Appends line with new line
				final_file += line + "\n"; 
			}
		}
		return final_file.split("\n");
	}
	
	public void run() {
		// Advances while there are still tokens left
		currentLine = fileContents[0];
		XMLFile += "<tokens>" + "\n";	// Opening tokens tag
		while (hasMoreTokens())	{
			currentToken = advance();
			String currentType = tokenType();

			XMLFile += "<" + currentType + "> ";	// Opening tag
			if (currentToken.contains("\"")) {
				// Replace quotes if string
				XMLFile += currentToken.replaceAll("\"", "");
			} else if (currentType.equals("symbol")) {
				XMLFile += getOp(currentToken);
			} else {
				XMLFile += currentToken;
			}
			XMLFile += " </" + currentType + ">" + "\n";	// Closing tag

			if (currentLine.length() == 1) {
				currentLineNum++;
				if (!hasMoreTokens()) {
					break;
				} else {	
					currentLine = fileContents[currentLineNum];	// Moves to next line
				}
			} else {
				// Removes first occurence of currentToken and any space
				String regex = Pattern.quote(currentToken) + "\\s?";
				currentLine = currentLine.replaceFirst(regex, "");
			}
		}
		XMLFile += "</tokens>" + "\n";	// Closing tokens tag
	}

	public boolean hasMoreTokens() {
		// No tokens are left when end of file and end of line reached
		if (currentLineNum == fileContents.length && 
			(currentLine.equals("") || currentLine.length() == 1)) {
			return false;
		} else {
			return true;
		}
	}
	
	public String advance() {
		// Advances to next token
		String[] line = currentLine.split("");
		String firstChar = line[0];

		if (isInt(firstChar)) {
			return regexFinder("\\d+", currentLine);	// Int token
		} else if (isOp(firstChar.charAt(0))) {
				return firstChar;	// Operation token
		} else if (firstChar.equals("\"")) {
			return regexFinder("\".*?\"", currentLine);	// String token
		} else {
			// Could be a keyword or identifier
			String token = "";
			// Loops until operation or space is reached
			for (String c : line) {
				if (c.equals(" ") || isOp(c.charAt(0))) {
					return token;
				} else {
					token += c;
				}
			}
		}
		return "error";
	}

	public String tokenType() {
		// Returns type of token
		if (isOp(currentToken.charAt(0))) {
			return "symbol";
		} else if (isKeyword(currentToken)) {
			return "keyword";
		} else if (currentToken.startsWith("\"")) {
			return "stringConstant";
		} else if (isInt(currentToken)) {
			return "integerConstant";
		} else {
			return "identifier";
		}
	}

	public boolean isOp(char testChar) {
		// Returns whether or not a character is an op-code
		for (char op : operations) {
			if (op == testChar) {
				return true;
			}
		}
		return false;
	}

	public boolean isKeyword(String word) {
		// Returns whether or not a string is a keyword
		for (String k : keywords) {
			if (k.equals(word)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInt(String test) {
		// Returns whether or not a string is a number
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matches = pattern.matcher(test);
		return matches.find();
	}

	public String getOp(String op) {
		// Returns op code unless op code
		// is < or >. < is &lt; and > is &gt;
		if (op.equals("<")) {
			return "&lt;";
		} else if (op.equals(">")) {
			return "&gt;";
		} else {
			return op;
		}
	}

	public String regexFinder(String regex, String text) {
		// Takes in a regex and returns matches
		Pattern pattern = Pattern.compile(regex);
		Matcher matches = pattern.matcher(text);
		matches.find();
		return matches.group();
	}

	public void writeToXml(String path) {
		// Writes to Xml file
		path = path.replace(".jack", "T2.xml"); // Changes file type
		try {
			FileWriter fileWrite = new FileWriter(path);
			fileWrite.write(XMLFile);
			fileWrite.close();
		} catch (IOException e) {
			System.out.println("Invalid path");
		}
	}
}