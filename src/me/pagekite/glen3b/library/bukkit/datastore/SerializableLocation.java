package me.pagekite.glen3b.library.bukkit.datastore;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

/**
 * Represents a YAML-serializable location class.
 * @author Glen Husman
 */
public final class SerializableLocation implements ConfigurationSerializable {

	private double x,y,z;
	private float yaw,pitch;
	private String world;
    
    /**
     * Creates a SerializableLocation from a location.
     * @param loc The location to serialize.
     */
    public SerializableLocation(Location loc) {
        Validate.notNull(loc, "The wrapped location must not be null.");
    	x=loc.getX();
        y=loc.getY();
        z=loc.getZ();
        yaw = loc.getYaw();
        pitch = loc.getPitch();
        world=loc.getWorld().getName();
    }
    
    /**
     * Gets the deserialized location.
     * @return A new Location instance representing this serialized state, or {@code null} if the serialized form was invalid.
     */
    public Location getLocation() {
        World w = Bukkit.getWorld(world);
        if(w==null)
            return null;
        Location toRet = new Location(w,x,y,z,yaw,pitch);
        return toRet;
    }
    
    /**
     * Internally used deserialization constructor. Should not be called by client code.
     * @see ConfigurationSerializable
     * @param serialized The serialized form of the object.
     */
  	public SerializableLocation(Map<String, Object> serialized){
  		Validate.notNull(serialized, "The object map is null.");
  		x = (Double)serialized.get("x");
  		y = (Double)serialized.get("y");
  		z = (Double)serialized.get("z");
  		world = (String)serialized.get("world");
  		
  		// Backwards compatibility with client code
  		// This is a MUST because server owners may have config files which may have old versions of this serialized class
  		// If that is the case, we must still be able to deserialize them
  		// Hence, we need to check for new keys and assume defaults if they don't exist
  		if(serialized.containsKey("yaw"))
  			yaw = (Float)serialized.get("yaw");
  		
  		if(serialized.containsKey("pitch"))
  			pitch = (Float)serialized.get("pitch");
  	}
    
  	/**
  	 * Serializes the object.
  	 * @see ConfigurationSerializable
  	 */
	@Override
	public Map<String, Object> serialize() {
		HashMap<String, Object> objects = new HashMap<String, Object>();
		objects.put("x", x);
		objects.put("y", y);
		objects.put("z", z);
		objects.put("world", world);
		objects.put("pitch", pitch);
		objects.put("yaw", yaw);
		return objects;
	}
}
