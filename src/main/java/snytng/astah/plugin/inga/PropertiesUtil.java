package snytng.astah.plugin.inga;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JRadioButton;

public class PropertiesUtil {

    private String fileName = null;
    public PropertiesUtil(String fileName) {
        this.fileName = fileName;
    }

    private Properties props = new Properties();

    public void readSetting(String key, JRadioButton rb) {
        if(props.getProperty(key) != null) {
            rb.setSelected(Boolean.parseBoolean(props.getProperty(key)));
        }
    }

    public void saveSetting(String key, JRadioButton rb) {
        props.setProperty(key, Boolean.toString(rb.isSelected()));
        savePropertiesToFile();
    }

    public void readSetting(String key, JComboBox<String> cb) {
        if(props.getProperty(key) != null) {
            cb.setSelectedIndex(Integer.parseInt(props.getProperty(key)));
        }
    }

    public void saveSetting(String key, JComboBox<String> cb) {
        props.setProperty(key, Integer.toString(cb.getSelectedIndex()));
        savePropertiesToFile();
    }

    private Path getPropertiesPath() {
        final String user_home = System.getProperty("user.home");
        final Path propsPath = Paths.get(user_home, this.fileName);
        return propsPath;
    }

    public void readPropertiesFromFile() {
        if(Files.exists(getPropertiesPath()) == false) {
            return;
        }

        try {
            props.load(Files.newInputStream(getPropertiesPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void savePropertiesToFile() {
        if(Files.exists(getPropertiesPath()) == false) {
            return;
        }

        try {
            props.store(Files.newOutputStream(getPropertiesPath()), "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
