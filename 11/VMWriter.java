import java.util.ArrayList;

/**
 * Implements VM Writer for jack compiler
 * 
 */ 
class VMWriter {
	ArrayList<String> outputVMFile;
	public VMWriter() {
		outputVMFile = new ArrayList<String>();
	}

	public static void main(String[] args) {}

	public void writePush(String segment, int index) {
		outputVMFile.add(String.format("push %s %d", segment, index));
	}

	public void writePop(String segment, int index) {
		outputVMFile.add(String.format("pop %s %d", segment, index));
	}

	public void writeArithmetic(String command) {
		outputVMFile.add(command);
	}

	public void writeLabel(String label) {
		outputVMFile.add(String.format("label %s", label));
	}

	public void writeGoto(String label) {
		outputVMFile.add(String.format("goto %s", label));
	}

	public void writeIf(String label) {
		outputVMFile.add(String.format("if-goto %s", label));
	}

	public void writeCall(String name, int nArgs) {
		outputVMFile.add(String.format("call %s %d", name, nArgs));
	}

	public void writeFunction(String name, int nLocals) {
		outputVMFile.add(String.format("function %s %d", name, nLocals));
	}

	public void writeReturn() {
		outputVMFile.add("return");
	}
}