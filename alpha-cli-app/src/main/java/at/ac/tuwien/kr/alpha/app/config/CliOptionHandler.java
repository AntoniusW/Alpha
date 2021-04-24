package at.ac.tuwien.kr.alpha.app.config;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

@FunctionalInterface
public interface CliOptionHandler<T> {

	void handleOption(Option opt, T dest) throws ParseException;

}
