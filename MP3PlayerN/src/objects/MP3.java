package objects;

import utils.FileUtils;
import java.io.Serializable;


/*
    nombre y ruta del mp3
*/
public class MP3 implements Serializable
{
    private String name;
    private String path;

    public MP3(String name, String path) 
    {
        this.name = name;
        this.path = path;
    }

    // para la correcta visualización del objeto  MP3 a la hora de añadir en la lista de reproducción
    @Override
    public String toString() 
    {
        return FileUtils.getFileNameWithoutExtension(name);
    }
   
    @Override
    public boolean equals(Object obj) 
    {
        if (!(obj instanceof MP3)) 
        {
            return false;
        }  
        MP3 mp3 = (MP3)obj;
        return path.equals(mp3.getPath());
    }
    
   
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public String getPath() 
    {
        return path;
    }
    
    public void setPath(String path) 
    {
        this.path = path;
    }
   
}
