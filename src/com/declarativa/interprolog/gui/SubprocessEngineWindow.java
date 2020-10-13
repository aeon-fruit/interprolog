/* 
Author: Miguel Calejo
Contact: info@interprolog.com, www.interprolog.com
Copyright InterProlog Consulting / Renting Point Lda, Portugal 2014
Use and distribution, without any warranties, under the terms of the
Apache License, as per http://www.apache.org/licenses/LICENSE-2.0.html
*/
package com.declarativa.interprolog.gui;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import com.declarativa.interprolog.PrologOutputListener;
import com.declarativa.interprolog.SubprocessEngine;

/** A ListenerWindow for a SubprocessEngine. Since Prolog runs as if under a regular OS shell, with standard I/O 
redirected to the ListenerWindow, this is the best to use during program development. */
@SuppressWarnings("serial")
public class SubprocessEngineWindow extends ListenerWindow implements PrologOutputListener{
	static final String TRIVIAL_PROLOG_OUTPUT = "(yes\\"+"n\\| \\?- \\"+"n)+";
	static Pattern prologSuccessPattern = Pattern.compile(TRIVIAL_PROLOG_OUTPUT,Pattern.MULTILINE);
	private boolean truncateOutput = true;
	// see mayTruncateEnd
	protected int lastTruncatePos = 0;
	
	protected Style stderrStyle,userInputStyle;
	
	public SubprocessEngineWindow(SubprocessEngine e){
		this(e,true,true,null);
	}
	public SubprocessEngineWindow(SubprocessEngine e,boolean autoDisplay){
		this(e,autoDisplay,true,null);
	}
	public SubprocessEngineWindow(SubprocessEngine e,boolean autoDisplay,boolean mayExitApp){
		this(e,autoDisplay,mayExitApp,null);
	}
	public SubprocessEngineWindow(SubprocessEngine e,boolean autoDisplay,boolean mayExitApp,Rectangle R){
		super(e,autoDisplay,mayExitApp,R);
		setTitle("SubprocessEngine listener ("+e.getPrologVersion()+")");
		((SubprocessEngine)engine).addPrologOutputListener(this); // so we get output and prompt "events"
	}
	
	protected void initializeOutputStyles() {
		userInputStyle = prologOutput.addStyle("userInput", null);
		StyleConstants.setForeground(userInputStyle, Color.BLUE);
		stderrStyle = prologOutput.addStyle("stderr", null);
		StyleConstants.setForeground(stderrStyle, Color.RED);
	}
	
	public void sendToProlog(String invisiblePostfix){
		String goal = prologInput.getText();
		prologOutput.append(goal+"\n",userInputStyle);
		if (invisiblePostfix == null)
			((SubprocessEngine)engine).sendAndFlushLn(goal);
		else
			((SubprocessEngine)engine).sendAndFlushLn(goal+invisiblePostfix);
		focusInput();
		addToHistory();
	}
	
	// PrologOutputListener methods:
	@Override
	public void print(String s){
	}
	@Override
	public void printStdout(String s) {
		if (debug) System.out.println("print("+s+")");
		prologOutput.append(s,null);
		mayTruncateEnd(); 
		scrollToBottom();
	}
	@Override
	public void printStderr(String s) {
		if (debug) System.out.println("print("+s+")");
		prologOutput.append(s,stderrStyle);
		mayTruncateEnd(); 
		scrollToBottom();
	}
	
	protected void mayTruncateEnd(Pattern successPattern,int smallestRemoval){
		if (!truncateOutput) return;
		//System.out.println("mayTruncateEnd:"+successPattern);
		if (prologOutput.getDocument().getLength()-lastTruncatePos < smallestRemoval) return;
		try{
			// We should probably fabricate a ligher CharSequence out of prologOutput's document, but so far no memory complaints here so...:
			String S = prologOutput.getDocument().getText(lastTruncatePos, prologOutput.getDocument().getLength()-lastTruncatePos);
			//System.out.println("Checking:"+S);
			Matcher M = successPattern.matcher(S);
			//System.out.println("M:"+M);
			if (M.find() && (M.end()-M.start())>smallestRemoval){
				prologOutput.getDocument().remove(M.start()+lastTruncatePos,M.end()-M.start());
				System.out.println("Removed "+(M.end()-M.start())+" redundant chars from listener window");
				engine.progressMessage("Removed "+(M.end()-M.start())+" redundant chars from listener window");
				lastTruncatePos = M.start()+lastTruncatePos;
				if (lastTruncatePos<0) lastTruncatePos = 0;
			}
		} catch ( BadLocationException e){
			System.err.println(e);
		}
	}
	protected void mayTruncateEnd(){
		mayTruncateEnd(prologSuccessPattern, TRIVIAL_PROLOG_OUTPUT.length()*2);
	}
	
	/** If you prefer to see all those "yes" after top level deterministicGoal calls, or if you're concerned with too much memory being spent
	on regular expression matching, call this */
	public void setTruncateOutput(boolean yes){
		if (yes&&!truncateOutput)
			lastTruncatePos=0;
		truncateOutput = yes;
	}
	
	public boolean isTruncateOutput(){
		return truncateOutput;
	}

}