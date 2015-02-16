package me.denley.preferenceinjector.internal;

/**
 * Created by Denley on 16/02/2015.
 */
public class PrefBinding implements Binding {
    private final String name;
    private final String type;
    private final boolean autoUpdate;

    public PrefBinding(String name, String type, boolean autoUpdate) {
        this.name = name;
        this.type = type;
        this.autoUpdate = autoUpdate;
    }

    @Override public String getName(){
        return name;
    }

    @Override public String getType(){
        return type;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    @Override public String getDescription() {
        return "field '" + name + "'";
    }

}
