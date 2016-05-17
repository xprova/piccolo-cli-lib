package net.xprova.simplecli;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
	public boolean enabled() default true;
	public String description() default "n/a";

}
