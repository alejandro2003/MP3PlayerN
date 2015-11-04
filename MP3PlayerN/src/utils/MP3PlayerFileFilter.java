package utils;

import java.io.File;
import javax.swing.filechooser.FileFilter;

//filtro para la posibilidad de elegir archivos con la extension mp3 
//para el componente FileChooser
public class MP3PlayerFileFilter extends FileFilter 
{
    private String fileExtension;
    private String fileDescription;

    public MP3PlayerFileFilter(String fileExtension, String fileDescription) 
    {
        this.fileExtension = fileExtension;
        this.fileDescription = fileDescription;
    }
  
    @Override
    public boolean accept(File file)
    {
        // permitir solo carpetas y archivos con la extension mp3
        return file.isDirectory() || file.getAbsolutePath().endsWith(fileExtension);
    }   

    @Override
    public String getDescription() 
    {// descripcion para el formato mp3 a la hora de elegir en la ventana del dialogo
        return fileDescription+" (*."+fileExtension+")";
    }
}
