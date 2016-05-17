package net.xprova.piccolo;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
	public boolean visible() default true;
	public boolean enabled() default true;
	public String description() default "n/a";
	public String[] aliases() default {};
}
