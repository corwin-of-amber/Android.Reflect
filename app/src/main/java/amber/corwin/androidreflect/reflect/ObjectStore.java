package amber.corwin.androidreflect.reflect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


public class ObjectStore {

	private class Entry {
		public long timestamp;
		public Object obj;
	}
	
	private Map<UUID, Entry> transientObjects = new HashMap<>();
	private Map<String, Object> persistentObjects = new HashMap<>();
	
	private long TRANSIENT_LIFE_SPAN = 10 * 60 * 1000000000;  /* 10 minutes (as nanoseconds) */


	public UUID add(Object o) {
		UUID u = UUID.randomUUID();
		Entry e = new Entry();
		e.timestamp = System.nanoTime();
		e.obj = o;
		transientObjects.put(u, e);
		return u;
	}
	
	public Object get(UUID uuid) {
		Entry e = transientObjects.get(uuid);
		return (e == null) ? null : e.obj;
	}
	
	public void persist(UUID uuid, String name) {
		persistentObjects.put(name, get(uuid));
	}
	
	public Object get(String name) {
		return persistentObjects.get(name);
	}
	
	/**
	 * Removes all transient objects older than TRANSIENT_LIFE_SPAN.
	 */
	public void cleanup() {
		long now = System.nanoTime();
		for (Iterator<Map.Entry<UUID, Entry>> ei = transientObjects.entrySet().iterator(); ei.hasNext(); ) {
			Entry e = ei.next().getValue();
			if (e.timestamp + TRANSIENT_LIFE_SPAN < now) ei.remove();
		}
	}
}
