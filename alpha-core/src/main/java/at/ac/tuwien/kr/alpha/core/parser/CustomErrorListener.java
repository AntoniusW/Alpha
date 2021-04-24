package at.ac.tuwien.kr.alpha.core.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class CustomErrorListener extends BaseErrorListener {
	RecognitionException recognitionException;

	private final String fileName;

	public CustomErrorListener(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
		super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);

		System.err.println(fileName + ":" + line + ":" + charPositionInLine + ": " + msg);

		this.recognitionException = e;
	}

	public RecognitionException getRecognitionException() {
		return recognitionException;
	}
}
