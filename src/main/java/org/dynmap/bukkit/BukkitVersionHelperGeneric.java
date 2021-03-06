package org.dynmap.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Server;
import org.bukkit.World;
import org.dynmap.Log;

/**
 * Helper for isolation of bukkit version specific issues
 */
public abstract class BukkitVersionHelperGeneric extends BukkitVersionHelper {
    private String obc_package; // Package used for org.bukkit.craftbukkit
    protected String nms_package; // Package used for net.minecraft.server
    private boolean failed;
    private static final Object[] nullargs = new Object[0];
    private static final Map nullmap = Collections.emptyMap();
    
    /** CraftChunkSnapshot */
    private Class<?> craftchunksnapshot;
    private Field ccss_biome;
    /** CraftChunk */
    private Class<?> craftchunk;
    private Method cc_gethandle;
    /** CraftWorld */
    private Class<?> craftworld;
    private Method cw_gethandle;

    /** BiomeBase related helpers */
    protected Class<?> biomebase;
    protected Class<?> biomebasearray;
    protected Field biomebaselist;
    protected Field biomebasetemp;
    protected Field biomebasehumi;
    protected Field biomebaseidstring;
    protected Field biomebaseid;
    /** n.m.s.World */
    protected Class<?> nmsworld;
    protected Class<?> chunkprovserver;
    protected Class<?> longhashset;
    protected Field nmsw_chunkproviderserver;
    protected Field cps_unloadqueue;
    protected Method lhs_containskey;
    /** n.m.s.Chunk */
    protected Class<?> nmschunk;
    protected Method nmsc_removeentities;
    protected Field nmsc_tileentities;
    protected Field nmsc_inhabitedticks;
    /** nbt classes */
    protected Class<?> nbttagcompound;
    protected Class<?> nbttagbyte;
    protected Class<?> nbttagshort;
    protected Class<?> nbttagint;
    protected Class<?> nbttaglong;
    protected Class<?> nbttagfloat;
    protected Class<?> nbttagdouble;
    protected Class<?> nbttagbytearray;
    protected Class<?> nbttagstring;
    protected Class<?> nbttagintarray;
    protected Method compound_get;
    protected Field nbttagbyte_val;
    protected Field nbttagshort_val;
    protected Field nbttagint_val;
    protected Field nbttaglong_val;
    protected Field nbttagfloat_val;
    protected Field nbttagdouble_val;
    protected Field nbttagbytearray_val;
    protected Field nbttagstring_val;
    protected Field nbttagintarray_val;
    /** Tile entity */
    protected Class<?> nms_tileentity;
    protected Method nmst_readnbt;
    protected Field nmst_x;
    protected Field nmst_y;
    protected Field nmst_z;

    BukkitVersionHelperGeneric() {
        failed = false;
        Server srv = Bukkit.getServer();
        /* Look up base classname for bukkit server - tells us OBC package */
        obc_package = Bukkit.getServer().getClass().getPackage().getName();
        /* Get NMS package */
        nms_package = getNMSPackage();
        if(nms_package == null) {
            failed = true;
        }
        /* Craftworld fields */
        craftworld = getOBCClass("org.bukkit.craftbukkit.CraftWorld");
        cw_gethandle = getMethod(craftworld, new String[] { "getHandle" }, new Class[0]);
        /* CraftChunkSnapshot */
        craftchunksnapshot = getOBCClass("org.bukkit.craftbukkit.CraftChunkSnapshot");
        ccss_biome = getPrivateField(craftchunksnapshot, new String[] { "biome" }, biomebasearray);
        /* CraftChunk */
        craftchunk = getOBCClass("org.bukkit.craftbukkit.CraftChunk");
        cc_gethandle = getMethod(craftchunk, new String[] { "getHandle" }, new Class[0]);
        /* Get NMS classes and fields */
        if(!failed)
            loadNMS();

        if(failed)
            throw new IllegalArgumentException("Error initializing dynmap - bukkit version incompatible!");
    }
    
    protected abstract void loadNMS();
    
    protected abstract String getNMSPackage();
    
    protected Class<?> getOBCClass(String classname) {
        return getClassByName(classname, "org.bukkit.craftbukkit", obc_package, false);
    }

    protected Class<?> getOBCClassNoFail(String classname) {
        return getClassByName(classname, "org.bukkit.craftbukkit", obc_package, true);
    }

    protected Class<?> getNMSClass(String classname) {
        return getClassByName(classname, "net.minecraft.server", nms_package, false);
    }
    
    protected Class<?> getClassByName(String classname, String base, String mapping, boolean nofail) {
        String n = classname;
        int idx = classname.indexOf(base);
        if(idx >= 0) {
            n = classname.substring(0, idx) + mapping + classname.substring(idx + base.length());
        }
        try {
            return Class.forName(n);
        } catch (ClassNotFoundException cnfx) {
            try {
                return Class.forName(classname);
            } catch (ClassNotFoundException cnfx2) {
                if(!nofail) {
                    Log.severe("Cannot find " + classname);
                    failed = true;
                }
                return null;
            }
        }
    }
    /**
     * Get field
     */
    protected Field getField(Class<?> cls, String[] ids, Class<?> type) {
        return getField(cls, ids, type, false);
    }
    protected Field getFieldNoFail(Class<?> cls, String[] ids, Class<?> type) {
        return getField(cls, ids, type, true);
    }
    /**
     * Get field
     */
    private Field getField(Class<?> cls, String[] ids, Class<?> type, boolean nofail) {
        if((cls == null) || (type == null)) return null;
        for(String id : ids) {
            try {
                Field f = cls.getField(id);
                if(f.getType().isAssignableFrom(type)) {
                    return f;
                }
            } catch (NoSuchFieldException nsfx) {
            }
        }
        if(!nofail) {
            Log.severe("Unable to find field " + ids[0] + " for " + cls.getName());
            failed = true;
        } 
        return null;
    }
    /**
     * Get private field
     */
    protected Field getPrivateField(Class<?> cls, String[] ids, Class<?> type) {
        if((cls == null) || (type == null)) return null;
        for(String id : ids) {
            try {
                Field f = cls.getDeclaredField(id);
                if(f.getType().isAssignableFrom(type)) {
                    f.setAccessible(true);
                    return f;
                }
            } catch (NoSuchFieldException nsfx) {
            }
        }
        Log.severe("Unable to find field " + ids[0] + " for " + cls.getName());
        failed = true;
        return null;
    }
    protected Object getFieldValue(Object obj, Field field, Object def) {
        if((obj != null) && (field != null)) {
            try {
                return field.get(obj);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            }
        }
        return def;
    }
    /**
     * Get method
     */
    protected Method getMethod(Class<?> cls, String[] ids, Class[] args) {
        if(cls == null) return null;
        for(String id : ids) {
            try {
                return cls.getMethod(id, args);
            } catch (SecurityException e) {
            } catch (NoSuchMethodException e) {
            }
        }
        Log.severe("Unable to find method " + ids[0] + " for " + cls.getName());
        failed = true;
        return null;
    }
    private Object callMethod(Object obj, Method meth, Object[] args, Object def) {
        if((obj == null) || (meth == null)) {
            return def;
        }
        try {
            return meth.invoke(obj, args);
        } catch (IllegalArgumentException iax) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        return def;
    }
    

    /**
     * Get list of defined biomebase objects
     */
    public Object[] getBiomeBaseList() {
        return (Object[]) getFieldValue(biomebase, biomebaselist, new Object[0]);
    }
    /** Get temperature from biomebase */
    public float getBiomeBaseTemperature(Object bb) {
        return (Float) getFieldValue(bb, biomebasetemp, 0.5F);
    }
    /** Get humidity from biomebase */
    public float getBiomeBaseHumidity(Object bb) {
        return (Float) getFieldValue(bb, biomebasehumi, 0.5F);
    }
    /** Get ID string from biomebase */
    public String getBiomeBaseIDString(Object bb) {
        return (String) getFieldValue(bb, biomebaseidstring, null);
    }
    /** Get ID from biomebase */
    public int getBiomeBaseID(Object bb) {
        return (Integer) getFieldValue(bb, biomebaseid, -1);
    }

    /* Get net.minecraft.server.world for given world */
    public Object getNMSWorld(World w) {
        return callMethod(w, cw_gethandle, nullargs, null);
    }

    /* Get unload queue for given NMS world */
    public Object getUnloadQueue(Object nmsworld) {
        Object cps = getFieldValue(nmsworld, nmsw_chunkproviderserver, null); // Get chunkproviderserver
        if(cps != null) {
            return getFieldValue(cps, cps_unloadqueue, null);
        }
        return null;
    }

    /* For testing unload queue for presence of givne chunk */
    public boolean isInUnloadQueue(Object unloadqueue, int x, int z) {
        if(unloadqueue != null) {
            return (Boolean)callMethod(unloadqueue, lhs_containskey, new Object[] { x, z }, true);
        }
        return true;
    }
    
    public Object[] getBiomeBaseFromSnapshot(ChunkSnapshot css) {
        return (Object[])getFieldValue(css, ccss_biome, null);
    }
    public boolean isCraftChunkSnapshot(ChunkSnapshot css) {
        if(craftchunksnapshot != null) {
            return craftchunksnapshot.isAssignableFrom(css.getClass());
        }
        return false;
    }
    /** Remove entities from given chunk */
    public void removeEntitiesFromChunk(Chunk c) {
        Object omsc = callMethod(c, cc_gethandle, nullargs, null);
        if(omsc != null) {
            callMethod(omsc, nmsc_removeentities, nullargs, null);
        }
    }
    /**
     * Get inhabited ticks count from chunk
     */
    private static final Long zero = new Long(0);
    public long getInhabitedTicks(Chunk c) {
        if (nmsc_inhabitedticks == null) {
            return 0;
        }
        Object omsc = callMethod(c, cc_gethandle, nullargs, null);
        if(omsc != null) {
            return (Long)getFieldValue(omsc, nmsc_inhabitedticks, zero);
        }
        return 0;
    }

    /** Get tile entities map from chunk */
    public Map getTileEntitiesForChunk(Chunk c) {
        Object omsc = callMethod(c, cc_gethandle, nullargs, null);
        if(omsc != null) {
            return (Map)getFieldValue(omsc, nmsc_tileentities, nullmap);
        }
        return nullmap;
    }
    /**
     * Get X coordinate of tile entity
     */
    public int getTileEntityX(Object te) {
        return (Integer)getFieldValue(te, nmst_x, 0);
    }
    /**
     * Get Y coordinate of tile entity
     */
    public int getTileEntityY(Object te) {
        return (Integer)getFieldValue(te, nmst_y, 0);
    }
    /**
     * Get Z coordinate of tile entity
     */
    public int getTileEntityZ(Object te) {
        return (Integer)getFieldValue(te, nmst_z, 0);
    }
    /**
     * Read tile entity NBT
     */
    public Object readTileEntityNBT(Object te) {
        if(nbttagcompound == null) return null;
        Object nbt = null;
        try {
            nbt = nbttagcompound.newInstance();
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        if(nbt != null) {
            callMethod(te, nmst_readnbt, new Object[] { nbt }, null);
        }
        return nbt;
    }
    /**
     * Get field value from NBT compound
     */
    public Object getFieldValue(Object nbt, String field) {
        Object val = callMethod(nbt, compound_get, new Object[] { field }, null);
        if(val == null) return null;
        Class<?> valcls = val.getClass();
        if(valcls.equals(nbttagbyte)) {
            return getFieldValue(val, nbttagbyte_val, null);
        }
        else if(valcls.equals(nbttagshort)) {
            return getFieldValue(val, nbttagshort_val, null);
        }
        else if(valcls.equals(nbttagint)) {
            return getFieldValue(val, nbttagint_val, null);
        }
        else if(valcls.equals(nbttaglong)) {
            return getFieldValue(val, nbttaglong_val, null);
        }
        else if(valcls.equals(nbttagfloat)) {
            return getFieldValue(val, nbttagfloat_val, null);
        }
        else if(valcls.equals(nbttagdouble)) {
            return getFieldValue(val, nbttagdouble_val, null);
        }
        else if(valcls.equals(nbttagbytearray)) {
            return getFieldValue(val, nbttagbytearray_val, null);
        }
        else if(valcls.equals(nbttagstring)) {
            return getFieldValue(val, nbttagstring_val, null);
        }
        else if(valcls.equals(nbttagintarray)) {
            return getFieldValue(val, nbttagintarray_val, null);
        }
        return null;
    }
}
