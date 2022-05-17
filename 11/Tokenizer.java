import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.MatchResult;

/**
 * Creates tokens for Jack file
 * Outputs to Xml file
 */ 
public class Tokenizer {
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

	public Tokenizer(String[] contents) {
		fileContents = contents;
		currentToken = "";
		currentLineNum = 0;
		XMLFile = "";
	}

	public static void main(String[] args) {}
	
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
				String regex = Pattern.quote(currentToken) + "\\s*";
				currentLine = currentLine.replaceFirst(regex, "");
			}
		}
		XMLFile += "</tokens>" + "\n";	// Closing tokens tag
	}

	private boolean hasMoreTokens() {
		// No tokens are left when end of file and end of line reached
		if (currentLineNum == fileContents.length && 
			(currentLine.equals("") || currentLine.length() == 1)) {
			return false;
		} else {
			return true;
		}
	}
	
	private String advance() {
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

	private String tokenType() {
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

	private String getOp(String op) {
		// Returns op code unless op code except for special chars
		// < is &lt; > is &gt; & is &amp; " is &quot;
		switch(op) {
			case "<":
				return "&lt;";
			case ">":
				return "&gt;";
			case "\"":
				return "&quot;";
			case "&":
				return "&amp;";
			default:
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
}