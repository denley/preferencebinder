package me.denley.preferenceinjector.internal;

/**
 * Created by Denley on 16/02/2015.
 */
public class MethodBinding implements Binding {
    private final String name;
    private final String type;
    private final boolean initialize;

    public MethodBinding(String name, String type, boolean initialize) {
        this.name = name;
        this.type = type;
        this.initialize = initialize;
    }

    @Override public String getName(){
        return name;
    }

    @Override public String getType(){
        return type;
    }

    public boolean isInitialize(){
        return initialize;
    }

    @Override public String getDescription() {
        return "method '" + name + "'";
    }

}
