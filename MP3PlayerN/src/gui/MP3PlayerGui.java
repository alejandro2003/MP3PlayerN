
package gui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel; 
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
//import javazoom.jlgui.player.amp.skin.Skin;

// clase para la reproduccón de los archivos mp3
import objects.MP3Player;
//mostrar archivo mp3: ruta y nombre
import objects.MP3;
//trabajo con los archivos
import utils.FileUtils;
//filtro para FileChooser para elegir solo archivos mp3
import utils.MP3PlayerFileFilter;
//para cambiar skin
import utils.SkinUtils;
import javax.swing.plaf.metal.*;


public class MP3PlayerGui extends javax.swing.JFrame implements BasicPlayerListener 
{

    //<editor-fold defaultstate="collapsed" desc="constantes">
    private static final String MP3_FILE_EXTENSION = "mp3";
    private static final String MP3_FILE_DESCRIPTION = "Archivos mp3";
    private static final String PLAYLIST_FILE_EXTENSION = "pls";
    private static final String PLAYLIST_FILE_DESCRIPTION = "Archivos de la lista de reproducción";
    private static final String EMPTY_STRING = "";
    private static final String INPUT_SONG_NAME = "Introduzca el nombre de la canción";
    //</editor-fold>
    
    private DefaultListModel mp3ListModel = new DefaultListModel();
    private FileFilter mp3FileFilter = new MP3PlayerFileFilter(MP3_FILE_EXTENSION, MP3_FILE_DESCRIPTION);
    private FileFilter playlistFileFilter = new MP3PlayerFileFilter(PLAYLIST_FILE_EXTENSION, PLAYLIST_FILE_DESCRIPTION);
    //reproduccion de archivos mp3
    private MP3Player player = new MP3Player(this);
    
    
    
    //<editor-fold defaultstate="collapsed" desc="variables para reproduccion de canciones">
    private long secondsAmount; // cuantos segundos ha pasado desde el comienzo de reproduccion
    private long duration; // duracion de cancion en segundos
    private int bytesLen; // tamaño de la canción en bytes
    private double posValue = 0.0; // posicion de la rueda
   
    private boolean movingFromJump = false;
    private boolean moveAutomatic = false; //para mover la rueda de reproduccion
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="listener de los eventos del reproductor BasicPlayerListener">
    @Override
    public void opened(Object o, Map map) 
    {

        // definir la longitud de la canción y el tamaño del archivo
        duration = (long) Math.round((((Long) map.get("duration")).longValue()) / 1000000);
        bytesLen = (int) Math.round(((Integer) map.get("mp3.length.bytes")).intValue());

        //si hay la etiqueta mp3  del nombre la cogemos, si no sacamos
        //el titulo del nombre del archivo
        String songName = map.get("title") != null ? map.get("title").toString():FileUtils.getFileNameWithoutExtension(new File(o.toString()).getName());

        // si el titulo es largo, lo cortamos
        if (songName.length() > 30)
        {
            songName = songName.substring(0, 30) + "...";
        }

        labelSongName.setText(songName);

    }
    
    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties)
    {
        
        float progress = -1.0f;
        if ((bytesread > 0) && ((duration > 0)))
        {
            progress = bytesread * 1.0f / bytesLen * 1.0f;
        }

        // cuantos segundos han pasado
        secondsAmount = (long)(duration * progress);
        if (duration != 0) 
        {
            if (movingFromJump == false) 
            {
                slideProgress.setValue(((int) Math.round(secondsAmount*1000/duration)));
            }
        }
           
    }

    @Override
    public void stateUpdated(BasicPlayerEvent bpe) 
    {
        int state = bpe.getCode();

        if (state == BasicPlayerEvent.PLAYING)
        {
            movingFromJump = false;
        } 
        else if (state == BasicPlayerEvent.SEEKING) 
        {
            movingFromJump = true;
        } 
        else if (state == BasicPlayerEvent.EOM)
        {
            if (selectNextSong())
            {
                playFile();
            }
        }
    }

    @Override
    public void setController(BasicController bc) {
    }
    //</editor-fold>

    /**
     * Creates new form MP3PlayerGui
     */
    public MP3PlayerGui() {
        initComponents();
    }

    private void playFile()
    {
        int[] indexPlayList = lstPlayList.getSelectedIndices();// recibimos indices elegidos(numero) de las canciones
        if (indexPlayList.length > 0)
        {// si se ha elegido por menos una cancion
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[0]);// encontramos primera cancion elegida 
            player.play(mp3.getPath());
            player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());
        }

    }

    private boolean selectPrevSong()
    {
        int nextIndex = lstPlayList.getSelectedIndex() - 1;
        if (nextIndex >= 0)
        {// si no hemos salido fuera de la lista de reproduccion
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }
        return false;
    }

    private boolean selectNextSong() 
    {
        int nextIndex = lstPlayList.getSelectedIndex() + 1;
        if (nextIndex <= lstPlayList.getModel().getSize() - 1)
        {// si no hemos salido fuera de la lista de reproduccion
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }
        return false;
    }

    private void searchSong() 
    {
        String searchStr = txtSearch.getText();

        //si en el buscador no hemos introducido nada - salir del metodo y no realizar la busqueda
        if (searchStr == null || searchStr.trim().equals(EMPTY_STRING))
        {
            return;
        }
        // todos los indices de los objetos, encontrados en la busqueda, van a ser guardados en la coleccion
        ArrayList<Integer> mp3FindedIndexes = new ArrayList<Integer>();
        // recorrremos la coleccion y buscamos las correspondencias de los nombres de las cancciones con la fila de busqueda
        for (int i = 0; i < mp3ListModel.size(); i++) 
        {
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(i);
            // поиск вхождения строки в название песни без учета регистра букв
            //buscar string en el nombre de la cancion sin considerar minusculas/mayusculas
            if (mp3.getName().toUpperCase().contains(searchStr.toUpperCase()))
            {
                mp3FindedIndexes.add(i);// indices encontramos añadimos en la coleccion
            }
        }

        // coleccion de indices guardamos en el array
        int[] selectIndexes = new int[mp3FindedIndexes.size()];

        if (selectIndexes.length == 0) 
        {// si no se encuentra ninguna cancion
            JOptionPane.showMessageDialog(this, "Поиск по строке \'" + searchStr + "\' не дал результатов");
            txtSearch.requestFocus();
            txtSearch.selectAll();
            return;
        }

        // transformar la coleccion en un array, (JList funciona con array)
        for (int i = 0; i < selectIndexes.length; i++) 
        {
            selectIndexes[i] = mp3FindedIndexes.get(i).intValue();
        }

        //seleccionar canciones encontradas en la lista segun el array de los indices
        lstPlayList.setSelectedIndices(selectIndexes);
    }

    private void processSeek(double bytes) 
    {
        try {
            long skipBytes = (long) Math.round(((Integer) bytesLen).intValue() * bytes);
            player.jump(skipBytes);
        } catch (Exception e) {
            e.printStackTrace();
            movingFromJump = false;
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        popupPlaylist = new javax.swing.JPopupMenu();
        popmenuAddSong = new javax.swing.JMenuItem();
        popmenuDeleteSong = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        popmenuOpenPlaylist = new javax.swing.JMenuItem();
        popmenuClearPlaylist = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        popmenuPlay = new javax.swing.JMenuItem();
        popmenuStop = new javax.swing.JMenuItem();
        popmenuPause = new javax.swing.JMenuItem();
        panelMain = new javax.swing.JPanel();
        btnStopSong = new javax.swing.JButton();
        btnPauseSong = new javax.swing.JButton();
        btnPlaySong = new javax.swing.JButton();
        btnNextSong = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstPlayList = new javax.swing.JList();
        slideVolume = new javax.swing.JSlider();
        tglbtnVolume = new javax.swing.JToggleButton();
        btnPrevSong = new javax.swing.JButton();
        btnAddSong = new javax.swing.JButton();
        btnDeleteSong = new javax.swing.JButton();
        btnSelectNext = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        btnSelectPrev = new javax.swing.JButton();
        slideProgress = new javax.swing.JSlider();
        labelSongName = new javax.swing.JLabel();
        panelSearch = new javax.swing.JPanel();
        btnSearch = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuOpenPlaylist = new javax.swing.JMenuItem();
        menuSavePlaylist = new javax.swing.JMenuItem();
        menuSeparator = new javax.swing.JPopupMenu.Separator();
        menuExit = new javax.swing.JMenuItem();
        menuPrefs = new javax.swing.JMenu();
        menuChangeSkin = new javax.swing.JMenu();
        menuSkin1 = new javax.swing.JMenuItem();
        menuSkin2 = new javax.swing.JMenuItem();
        menuSkin4 = new javax.swing.JMenuItem();
        menuSkin3 = new javax.swing.JMenuItem();

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle("Выбрать файл");
        fileChooser.setMultiSelectionEnabled(true);

        popmenuAddSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/plus_16.png"))); // NOI18N
        popmenuAddSong.setText("Añadir la canción");
        popmenuAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuAddSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuAddSong);

        popmenuDeleteSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove_icon.png"))); // NOI18N
        popmenuDeleteSong.setText("Eliminar la canción");
        popmenuDeleteSong.setToolTipText("");
        popmenuDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuDeleteSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuDeleteSong);
        popupPlaylist.add(jSeparator1);

        popmenuOpenPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-icon.png"))); // NOI18N
        popmenuOpenPlaylist.setLabel("Abrir la lista de reproducción");
        popmenuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuOpenPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuOpenPlaylist);

        popmenuClearPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/clear.png"))); // NOI18N
        popmenuClearPlaylist.setLabel("Borrar la lista de reproducción");
        popmenuClearPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuClearPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuClearPlaylist);
        popupPlaylist.add(jSeparator3);

        popmenuPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Play.png"))); // NOI18N
        popmenuPlay.setText("Play");
        popmenuPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPlayActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPlay);

        popmenuStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/stop-red-icon.png"))); // NOI18N
        popmenuStop.setText("Stop");
        popmenuStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuStopActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuStop);

        popmenuPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Pause-icon.png"))); // NOI18N
        popmenuPause.setText("Pause");
        popmenuPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPauseActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPause);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MP3 плеер");
        setResizable(false);

        panelMain.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnStopSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/stop-red-icon.png"))); // NOI18N
        btnStopSong.setToolTipText("Detener");
        btnStopSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopSongActionPerformed(evt);
            }
        });

        btnPauseSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Pause-icon.png"))); // NOI18N
        btnPauseSong.setToolTipText("Pausar");
        btnPauseSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseSongActionPerformed(evt);
            }
        });

        btnPlaySong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Play.png"))); // NOI18N
        btnPlaySong.setToolTipText("Reproducir");
        btnPlaySong.setName("btnPlay"); // NOI18N
        btnPlaySong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlaySongActionPerformed(evt);
            }
        });

        btnNextSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/next-icon.png"))); // NOI18N
        btnNextSong.setToolTipText("La canción siguiente");
        btnNextSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextSongActionPerformed(evt);
            }
        });

        lstPlayList.setModel(mp3ListModel);
        lstPlayList.setToolTipText("La lista de las canciones");
        lstPlayList.setComponentPopupMenu(popupPlaylist);
        lstPlayList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstPlayListMouseClicked(evt);
            }
        });
        lstPlayList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                lstPlayListKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(lstPlayList);

        slideVolume.setMaximum(200);
        slideVolume.setMinorTickSpacing(5);
        slideVolume.setSnapToTicks(true);
        slideVolume.setToolTipText("Cambiar el volumen");
        slideVolume.setValue(100);
        slideVolume.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideVolumeStateChanged(evt);
            }
        });

        tglbtnVolume.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/speaker.png"))); // NOI18N
        tglbtnVolume.setToolTipText("Apagar el volumen");
        tglbtnVolume.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mute.png"))); // NOI18N
        tglbtnVolume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tglbtnVolumeActionPerformed(evt);
            }
        });

        btnPrevSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/prev-icon.png"))); // NOI18N
        btnPrevSong.setToolTipText("La canción anterior");
        btnPrevSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrevSongActionPerformed(evt);
            }
        });

        btnAddSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/plus_16.png"))); // NOI18N
        btnAddSong.setToolTipText("Añadir una canción");
        btnAddSong.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnAddSong.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnAddSong.setName("btnAddSong"); // NOI18N
        btnAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddSongActionPerformed(evt);
            }
        });

        btnDeleteSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove_icon.png"))); // NOI18N
        btnDeleteSong.setToolTipText("Eliminar una canción");
        btnDeleteSong.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnDeleteSong.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnDeleteSong.setName("btnAddSong"); // NOI18N
        btnDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteSongActionPerformed(evt);
            }
        });

        btnSelectNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/arrow-down-icon.png"))); // NOI18N
        btnSelectNext.setToolTipText("Destacar la siguiente canción");
        btnSelectNext.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnSelectNext.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnSelectNext.setName("btnAddSong"); // NOI18N
        btnSelectNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectNextActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        btnSelectPrev.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/arrow-up-icon.png"))); // NOI18N
        btnSelectPrev.setToolTipText("Выделить предыдущую песню");
        btnSelectPrev.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnSelectPrev.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnSelectPrev.setName("btnAddSong"); // NOI18N
        btnSelectPrev.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        btnSelectPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectPrevActionPerformed(evt);
            }
        });

        slideProgress.setMaximum(1000);
        slideProgress.setMinorTickSpacing(1);
        slideProgress.setSnapToTicks(true);
        slideProgress.setToolTipText("Mover");
        slideProgress.setValue(0);
        slideProgress.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideProgressStateChanged(evt);
            }
        });

        labelSongName.setText("...");
        labelSongName.setName(""); // NOI18N

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(btnAddSong, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDeleteSong)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(37, 37, 37)
                        .addComponent(btnSelectNext, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSelectPrev, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(slideProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(tglbtnVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addComponent(btnPrevSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnPlaySong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnPauseSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnStopSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnNextSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addComponent(slideVolume, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                    .addComponent(labelSongName))
                .addContainerGap())
        );
        panelMainLayout.setVerticalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnSelectNext, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jSeparator2)
                        .addComponent(btnSelectPrev, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(btnAddSong, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnDeleteSong, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(labelSongName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(slideProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(slideVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tglbtnVolume))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnPlaySong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnPauseSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnStopSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnNextSong, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnPrevSong, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(68, Short.MAX_VALUE))
        );

        btnDeleteSong.getAccessibleContext().setAccessibleDescription("Eliminar una canción");
        btnSelectPrev.getAccessibleContext().setAccessibleDescription("Destacar la anterior canción");

        panelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/search_16.png"))); // NOI18N
        btnSearch.setText("Encontrar");
        btnSearch.setToolTipText("Encontrar la canción");
        btnSearch.setActionCommand("search");
        btnSearch.setName("btnSearch"); // NOI18N
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        txtSearch.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        txtSearch.setForeground(new java.awt.Color(153, 153, 153));
        txtSearch.setText("Introduzca nombre de la canción");
        txtSearch.setToolTipText("");
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtSearchFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtSearchFocusLost(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout panelSearchLayout = new javax.swing.GroupLayout(panelSearch);
        panelSearch.setLayout(panelSearchLayout);
        panelSearchLayout.setHorizontalGroup(
            panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtSearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearch)
                .addContainerGap())
        );
        panelSearchLayout.setVerticalGroup(
            panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSearchLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch))
                .addContainerGap())
        );

        menuFile.setText("Archivo");

        menuOpenPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-icon.png"))); // NOI18N
        menuOpenPlaylist.setText("Abrir la lista de reproducción");
        menuOpenPlaylist.setName(""); // NOI18N
        menuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenPlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenPlaylist);

        menuSavePlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/save_16.png"))); // NOI18N
        menuSavePlaylist.setText("Guardar la lista de reproducción");
        menuSavePlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSavePlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuSavePlaylist);
        menuFile.add(menuSeparator);

        menuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/exit.png"))); // NOI18N
        menuExit.setText("Salir");
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        jMenuBar1.add(menuFile);

        menuPrefs.setText("Herramientas");

        menuChangeSkin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/gear_16.png"))); // NOI18N
        menuChangeSkin.setText("Skins");

        menuSkin1.setText("Estándar");
        menuSkin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin1ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin1);

        menuSkin2.setText("Nimbus");
        menuSkin2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin2ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin2);

        menuSkin4.setText("Motif");
        menuSkin4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin4ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin4);

        menuSkin3.setText("Metal");
        menuSkin3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin3ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin3);

        menuPrefs.add(menuChangeSkin);

        jMenuBar1.add(menuPrefs);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        setSize(new java.awt.Dimension(366, 627));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnPlaySongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlaySongActionPerformed
        playFile();
    }//GEN-LAST:event_btnPlaySongActionPerformed

    private void btnStopSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopSongActionPerformed
        player.stop();
    }//GEN-LAST:event_btnStopSongActionPerformed

    private void btnPauseSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseSongActionPerformed
        player.pause();
    }//GEN-LAST:event_btnPauseSongActionPerformed

    private void slideVolumeStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slideVolumeStateChanged
        player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());

        if (slideVolume.getValue() == 0) 
        {
            tglbtnVolume.setSelected(true);
        } 
        else 
        {
            tglbtnVolume.setSelected(false);
        }
    }//GEN-LAST:event_slideVolumeStateChanged

    private void btnNextSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextSongActionPerformed
        if (selectNextSong())
        {
            playFile();
        }
    }//GEN-LAST:event_btnNextSongActionPerformed

    private void btnPrevSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrevSongActionPerformed
        if (selectPrevSong())
        {
            playFile();
        }
    }//GEN-LAST:event_btnPrevSongActionPerformed

    private void btnSelectPrevActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectPrevActionPerformed
        selectPrevSong();
    }//GEN-LAST:event_btnSelectPrevActionPerformed

    private void btnSelectNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectNextActionPerformed
        selectNextSong();
    }//GEN-LAST:event_btnSelectNextActionPerformed

    private void btnAddSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddSongActionPerformed
        FileUtils.addFileFilter(fileChooser, mp3FileFilter);
        int result = fileChooser.showOpenDialog(this);// result guarda el resultado: el archivo esta elegido o no

        if (result == JFileChooser.APPROVE_OPTION) 
        {// si esta pulsado el boton OK o YES
            File[] selectedFiles = fileChooser.getSelectedFiles();
            // para añadir canciones seleccionadas en playlist
            for (File file:selectedFiles) 
            {
                MP3 mp3 = new MP3(file.getName(), file.getPath());

                // si esta cancion ya esta en la lista - no añadirla
                if (!mp3ListModel.contains(mp3))
                {
                    mp3ListModel.addElement(mp3);
                }
            }

        }
    }//GEN-LAST:event_btnAddSongActionPerformed

    private void btnDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteSongActionPerformed
        int[] indexPlayList = lstPlayList.getSelectedIndices();// recibimos los indices de canciones
        if (indexPlayList.length > 0) 
        {// si está selsccionada por menos una canción
            ArrayList<MP3> mp3ListForRemove = new ArrayList<MP3>();// primero guardamos todas canciones  para ser eliminadas en una coleccion separada
            for (int i = 0; i < indexPlayList.length; i++) 
            {// eliminamos todas canciones seleccionadas del playlist
                MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[i]);
                mp3ListForRemove.add(mp3);
            }

            // eliminamos mp3 en la lista de reproduccion
            for (MP3 mp3 : mp3ListForRemove) 
            {
                mp3ListModel.removeElement(mp3);
            }

        }
    }//GEN-LAST:event_btnDeleteSongActionPerformed

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchActionPerformed
        searchSong();

    }//GEN-LAST:event_btnSearchActionPerformed

    private void txtSearchFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusGained
        if (txtSearch.getText().equals(INPUT_SONG_NAME)) {
            txtSearch.setText(EMPTY_STRING);
        }
    }//GEN-LAST:event_txtSearchFocusGained

    private void txtSearchFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusLost
        if (txtSearch.getText().trim().equals(EMPTY_STRING)) {
            txtSearch.setText(INPUT_SONG_NAME);
        }
    }//GEN-LAST:event_txtSearchFocusLost

    private void menuSavePlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSavePlaylistActionPerformed
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {// si esta pulsado el boton OK o YES
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) 
            {// si ese archivo ya existe

                int resultOvveride = JOptionPane.showConfirmDialog(this, "Файл существует", "Перезаписать?", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (resultOvveride)
                {
                    case JOptionPane.NO_OPTION:
                        menuSavePlaylistActionPerformed(evt);// otra vez abrir para gurdar el archivo
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        fileChooser.cancelSelection();
                        return;
                }
                fileChooser.approveSelection();
            }

            String fileExtension = FileUtils.getFileExtension(selectedFile);

            // nombre de archivo (нcn extensión)
            String fileNameForSave = (fileExtension != null && fileExtension.equals(PLAYLIST_FILE_EXTENSION)) ? selectedFile.getPath() : selectedFile.getPath() + "." + PLAYLIST_FILE_EXTENSION;

            FileUtils.serialize(mp3ListModel, fileNameForSave);
        }

    }//GEN-LAST:event_menuSavePlaylistActionPerformed

    private void menuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenPlaylistActionPerformed
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showOpenDialog(this);// si el archivo está elegido o no


        if (result == JFileChooser.APPROVE_OPTION) 
        {// si OK o YES
            File selectedFile = fileChooser.getSelectedFile();// 
            DefaultListModel mp3ListModel = (DefaultListModel) FileUtils.deserialize(selectedFile.getPath());
            this.mp3ListModel = mp3ListModel;
            lstPlayList.setModel(mp3ListModel);
        }


    }//GEN-LAST:event_menuOpenPlaylistActionPerformed

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_menuExitActionPerformed

    private void menuSkin2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin2ActionPerformed
        SkinUtils.changeSkin(this, new NimbusLookAndFeel());
        fileChooser.updateUI();
    }//GEN-LAST:event_menuSkin2ActionPerformed

    private void lstPlayListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstPlayListMouseClicked

        // si boton izquierdo 2 veces pulsado
        if (evt.getModifiers() == InputEvent.BUTTON1_MASK && evt.getClickCount() == 2) {
            playFile();
        }
    }//GEN-LAST:event_lstPlayListMouseClicked
    private int currentVolumeValue;
    private void tglbtnVolumeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tglbtnVolumeActionPerformed

        if (tglbtnVolume.isSelected()) 
        {
            currentVolumeValue = slideVolume.getValue();
            slideVolume.setValue(0);
        } 
        else 
        {
            slideVolume.setValue(currentVolumeValue);
        }
    }//GEN-LAST:event_tglbtnVolumeActionPerformed

    private void popmenuAddSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuAddSongActionPerformed
        btnAddSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuAddSongActionPerformed

    private void popmenuDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuDeleteSongActionPerformed
        btnDeleteSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuDeleteSongActionPerformed

    private void popmenuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuOpenPlaylistActionPerformed
        menuOpenPlaylistActionPerformed(evt);
    }//GEN-LAST:event_popmenuOpenPlaylistActionPerformed

    private void popmenuClearPlaylistActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuClearPlaylistActionPerformed
        mp3ListModel.clear();
    }//GEN-LAST:event_popmenuClearPlaylistActionPerformed

    private void slideProgressStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_slideProgressStateChanged

        if (slideProgress.getValueIsAdjusting() == false) 
        {
            if (moveAutomatic == true) 
            {
                moveAutomatic = false;
                posValue = slideProgress.getValue() * 1.0 / 1000;
                processSeek(posValue);
            }
        } 
        else 
        {
            moveAutomatic = true;
            movingFromJump = true;
        }

    }//GEN-LAST:event_slideProgressStateChanged

    private void popmenuPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuPlayActionPerformed
        btnPlaySongActionPerformed(evt);
    }//GEN-LAST:event_popmenuPlayActionPerformed

    private void popmenuStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuStopActionPerformed
        btnStopSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuStopActionPerformed

    private void popmenuPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popmenuPauseActionPerformed
        btnPauseSongActionPerformed(evt);
    }//GEN-LAST:event_popmenuPauseActionPerformed

    private void lstPlayListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_lstPlayListKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            playFile();
        }
    }//GEN-LAST:event_lstPlayListKeyPressed

    private void txtSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            searchSong();
        }
    }//GEN-LAST:event_txtSearchKeyPressed

    private void menuSkin1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin1ActionPerformed
        SkinUtils.changeSkin(this, UIManager.getSystemLookAndFeelClassName());
        fileChooser.updateUI();
    }//GEN-LAST:event_menuSkin1ActionPerformed

    private void menuSkin3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin3ActionPerformed
        SkinUtils.changeSkin(this, "javax.swing.plaf.metal.MetalLookAndFeel");
        fileChooser.updateUI();
    }//GEN-LAST:event_menuSkin3ActionPerformed

    private void menuSkin4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSkin4ActionPerformed
        SkinUtils.changeSkin(this, "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        fileChooser.updateUI();
    }//GEN-LAST:event_menuSkin4ActionPerformed
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;


                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MP3PlayerGui().setVisible(true);
                //MP3PlayerGui().loadJS();
            }
        });
    }
    
    
   
    
    
    
    
    
    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddSong;
    private javax.swing.JButton btnDeleteSong;
    private javax.swing.JButton btnNextSong;
    private javax.swing.JButton btnPauseSong;
    private javax.swing.JButton btnPlaySong;
    private javax.swing.JButton btnPrevSong;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSelectNext;
    private javax.swing.JButton btnSelectPrev;
    private javax.swing.JButton btnStopSong;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JLabel labelSongName;
    private javax.swing.JList lstPlayList;
    private javax.swing.JMenu menuChangeSkin;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenuItem menuOpenPlaylist;
    private javax.swing.JMenu menuPrefs;
    private javax.swing.JMenuItem menuSavePlaylist;
    private javax.swing.JPopupMenu.Separator menuSeparator;
    private javax.swing.JMenuItem menuSkin1;
    private javax.swing.JMenuItem menuSkin2;
    private javax.swing.JMenuItem menuSkin3;
    private javax.swing.JMenuItem menuSkin4;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelSearch;
    private javax.swing.JMenuItem popmenuAddSong;
    private javax.swing.JMenuItem popmenuClearPlaylist;
    private javax.swing.JMenuItem popmenuDeleteSong;
    private javax.swing.JMenuItem popmenuOpenPlaylist;
    private javax.swing.JMenuItem popmenuPause;
    private javax.swing.JMenuItem popmenuPlay;
    private javax.swing.JMenuItem popmenuStop;
    private javax.swing.JPopupMenu popupPlaylist;
    public static javax.swing.JSlider slideProgress;
    private javax.swing.JSlider slideVolume;
    private javax.swing.JToggleButton tglbtnVolume;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}