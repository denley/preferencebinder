package me.denley.preferenceinjector.internal;

/**
 * Created by Denley on 16/02/2015.
 */
public class MethodBinding implements Binding {
    private final String name;
    private final String type;

    public MethodBinding(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }

    @Override public String getDescription() {
        return "field '" + name + "'";
    }

}
