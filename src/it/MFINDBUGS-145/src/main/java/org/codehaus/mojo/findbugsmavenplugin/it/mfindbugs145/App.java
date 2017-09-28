package com.github.spotbugs.spotbugs-maven-plugin.it.mfindbugs145;

/**
 * Hello world!
 *
 */
public class App implements Cloneable
{

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }

    public Object clone()
    {
        return null; // Does not call 'super.clone()'.
    }

}
