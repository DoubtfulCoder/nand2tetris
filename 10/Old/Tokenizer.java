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
public class Tokenizer {
	String[] fileContents;
	String currentToken;
	String[] keywords = {"class", "constructor", "function", "method", "field",
					"static", "var", "int", "char", "boolean", "void",
					"true", "false", "null", "this", "let", "do", "if", 
					"else", "while", "return"};
	String currentLine;
	int currentLineNum;
	String XMLFile;

	public Tokenizer(String[] contents) {
		fileContents = contents;
		currentToken = "";
		String[] keywords = {"class", "constructor", "function", "method", "field",
					"static", "var", "int", "char", "boolean", "void",
					"true", "false", "null", "this", "let", "do", "if", 
					"else", "while", "return"};
		char[] operations = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', 
						'-', '*', '/', '&', '|', '<', '>', '=', '~'};
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
					Tokenizer file = new Tokenizer(file_no_whitespace);
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
		// Pattern pattern = Pattern.compile("/\\*\\*?[\\s.]*\\*/");
		// file = file.find("/\\*\\*?.*")
		// Pattern pattern = Pattern.compile("/\\*\\*?.*");
		// // Pattern pattern = Pattern.compile("This file");
		// Matcher matches = pattern.matcher(file);
		// while (matches.find()) 
		// {
		// 	System.out.println(matches.group());
		// }
		return final_file.split("\n");
	}
	
	public void run() {
		// Advances while there are still tokens left
		currentLine = fileContents[0];
		XMLFile += "<tokens>" + "\n";
		while (hasMoreTokens())	{
			if (currentLine.split("")[0].equals(" ")) {
				// Doesn't tokenize spaces
				currentLine = currentLine.replaceFirst(" ", "");
			}

			currentToken = advance();
			String currentType = tokenType();

			XMLFile += "<" + currentType + "> ";
			if (currentToken.contains("\"")) {
				// Replace string character if string
				XMLFile += currentToken.replaceAll("\"", "");
			} else if (currentType.equals("symbol")) {
				XMLFile += getOp(currentToken);
			} else {
				XMLFile += currentToken;
			}
			XMLFile += " </" + currentType + ">" + "\n";

			if (currentLine.length() == 1) {
				currentLineNum++;
				if (!hasMoreTokens()) {
					break;
				} else {	
					currentLine = fileContents[currentLineNum];
				}
			} else {
				// Removes first occurence of currentToken and optional space
				// String x = "\"HOW MANY NUMBERS? \");";
				// System.out.println(x.replaceFirst("\"HOW MANY NUMBERS? \\s?", ""));
				// String regex = currentToken + "\\s?";
				// Escapes potential symbols like ( or ]
				// regex = reservedReg(currentToken) ? ("\\" + regex) : regex;
				String regex = Pattern.quote(String.format("%s", currentToken));
				currentLine = currentLine.replaceFirst(regex, "");
				// currentLine = currentLine.replaceFirst(regex, "");
			}
		}
		XMLFile += "</tokens>" + "\n";
	}

	public boolean hasMoreTokens() {
		if (currentLineNum == fileContents.length && 
			(currentLine.equals("") || currentLine.length() == 1)) {
			return false;
		} else {
			return true;
		}
	}
	
	public String advance() {
		String[] line = currentLine.split("");
		String firstChar = line[0];

		if (isInt(firstChar)) {
			String token = "";
			for (String c : line) {
				if (isInt(c)) {
					token = token + c;
				} else {
					return token;
				}
			}
		} else {
			if (isOp(firstChar.charAt(0))) {
				return firstChar;
			}
			else if (firstChar.equals("\"")) {
				String token = "\"";
				for (int i = 1; i < line.length; i++) {
					if (line[i].equals("\"")) {
						return token + "\"";
					} else {
						token += line[i];
					}
				}
			}
			else {
				String token = "";
				for (String c : line) {
					if (c.equals(" ") || isOp(c.charAt(0))) {
						return token;
					} else {
						token += c;
					}
				}
			}
		}
		return "wah";
	}

	public String tokenType() {
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
		char[] operations = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', 
						'-', '*', '/', '&', '|', '<', '>', '=', '~'};
		// Returns whether or not a character is an op-code
		for (char op : operations) {
			if (op == testChar) {
				return true;
			}
		}
		return false;
	}

	public boolean isKeyword(String word) {
		for (String k : keywords) {
			if (k.equals(word)) {
				return true;
			}
		}
		return false;
	}

	public boolean isInt(String test) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matches = pattern.matcher(test);
		return matches.find();
		// Pattern pattern = Pattern.compile("/\\*\\*?.*");
		// // Pattern pattern = Pattern.compile("This file");
		// Matcher matches = pattern.matcher(file);
		// while (matches.find()) 
		// {
		// 	System.out.println(matches.group());
		// }

		// try {
		// 	Integer.parseInt(test);
		// 	return true;
		// } catch (NumberFormatException e) {
		// 	return false;
		// }
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

	public static boolean reservedReg(String symbol) {
		// Returns whether a symbol is a reserved regex char such as {
		Pattern pattern = Pattern.compile("[\\(\\)\\{\\}\\[\\(\\]\\.\\+\\*]");
		Matcher matches = pattern.matcher(symbol);
		return matches.find();
	}

	public void writeToXml(String path) {
		// Writes to Xml file
		path = path.replace(".jack", "T2.xml"); // Changes file type
		try {
			FileWriter fileWrite = new FileWriter(path);
			fileWrite.write(XMLFile);
			fileWrite.close();
		} catch (IOException e) {
			System.out.println("Invalid path for writing");
		}
	}
}