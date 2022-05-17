// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
//
// This program only needs to handle arguments that satisfy
// R0 >= 0, R1 >= 0, and R0*R1 < 32768.

// Put your code here.

@R0
D=M
@n 
M=D // n = R0
@R1
D=M
@i
M=D // i=R1
@mult
M=0 // mult=0

(LOOP)
	@i
	D=M
	@STOP
	D;JEQ // stop if i is 0

	@n
	D=M
	@mult
	M=M+D // adding R0
	@i
	M=M-1 // iterating down
	@LOOP
	0;JMP // unconditional jump back to loop

(STOP)
	@mult
	D=M
	@R2
	M=D // storing final result in R2

(END)
	@END
	0;JMP