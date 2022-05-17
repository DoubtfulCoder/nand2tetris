// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
(LISTEN)
	@SCREEN
	D=A
	@addr
	M=D // screen start value
	@24576
	D=A
	@total
	M=D // total rows
	@color
	M=0 // default white color
	
	@KBD
	D=M
	@DRAW
	D;JEQ // jump if no key pressed

	@color
	M=-1 // set to black if key pressed
	@DRAW
	0;JMP

(DRAW)
	@addr
	D=M
	@total
	D=M-D
	@LISTEN
	D;JEQ // stop drawing if finished

	@color
	D=M // grabbing color
	@addr
	A=M // pointer: selecting address to change color e.g. 21242
	M=D // setting color of address

	@addr
	M=M+1 // next address

	@DRAW
	0;JMP // next iteration