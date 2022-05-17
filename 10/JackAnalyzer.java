import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Opens file or directory of jack files
 * Produces tokens with tokenizer
 * Sends to CompilationEngine
 * Outputs to XML file
 */
class JackAnalyzer {
	public static void main(String[] args) {
        // Opens either a file or directory;
        // If directory, opens all jack files in dir
        String fileDirName = args[0];
        if (fileDirName.indexOf('.') != -1) {
            tokenizeCompileFile(fileDirName);
        } else {
            // Opens each file in directory
            File dir = new File(fileDirName);
            File[] filesInDir = dir.listFiles();
            for (File f : filesInDir) {
                String filePath = f.getAbsolutePath();
                if (filePath.endsWith("jack")) {
                    tokenizeCompileFile(filePath);
                }
            }
        }
	}

    public static void tokenizeCompileFile(String filePath) {
        // Opens files, removes whitespace tokenizes, compiles, and outputs
        // Removes whitespace from file and tokenizes
        String[] file_no_whitespace = whiteSpaceRem(openFile(filePath));
        Tokenizer file = new Tokenizer(file_no_whitespace);
        file.run();
        String tokenizerOutput = file.XMLFile;
        writeToXml(filePath, tokenizerOutput, "T.xml");

        // Removes tokens tags and compiles
        tokenizerOutput = tokenizerOutput.replace("<tokens>\n", "");
        tokenizerOutput = tokenizerOutput.replace("</tokens>", "");
        CompilationEngine compilation = new CompilationEngine(tokenizerOutput.split("\n"));
        compilation.compileClass();

        // Outputs to XML file
        ArrayList<String> outputFile = compilation.outputFile;
        String finalOutput = "";
        int outputSize = outputFile.size();
        for (int i = 0; i < outputSize; i++) {
            finalOutput += outputFile.get(i) + "\n";
        }
        writeToXml(filePath, finalOutput, ".xml");
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

    public static String[] whiteSpaceRem(String[] file) {
        // Removes all whitespace and comments
        String final_file = "";
        for (String line : file) {
            // Removes inline comments
            line = line.replaceFirst("//.*", "");
            // Removes trailing whitespace
            line = line.strip();
            // Only appends non-empty and non-comment lines
            if (!(line.startsWith("/*") || line.startsWith("*") || line.length() == 0)) {
                // Appends line with new line
                final_file += line + "\n"; 
            }
        }
        return final_file.split("\n");
    }

    public static void writeToXml(String path, String XMLFile, String newFile) {
        // Writes to Xml file
        path = path.replace(".jack", newFile); // Changes file type
        try {
            FileWriter fileWrite = new FileWriter(path);
            fileWrite.write(XMLFile);
            fileWrite.close();
        } catch (IOException e) {
            System.out.println("Invalid path");
        }
    }
}