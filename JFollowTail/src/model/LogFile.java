package model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import interfaces.ModelListener;

public class LogFile implements PropertyChangeListener {
	private static final String NO_LOG_FILE = "No log file";
	private static final String TAB_IN_SPACES = "        ";
	private File file;
	private FileListener fileListener;
	private PropertyChangeSupport propertyChangeSupport;
	private ModelListener modelListener = new NullListener();// Default Listener
																// - do nothing

	public LogFile(File file) throws IOException {
		propertyChangeSupport = new PropertyChangeSupport(this);
		setFile(file);
	}

	public void setModelListener(ModelListener listener) {
		this.modelListener = listener;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) throws IOException {
		this.file = file;
		createFileListener();

	}

	public void stopCurrentFileListener() {
		if (fileListener != null) {
			fileListener.stop();
		}
	}

	public void load() throws IOException {
		loadFile(0);
		modelListener.done();
	}

	private synchronized void loadFile(long skip) throws IOException {

		try (Stream<String> lines = Files.lines(Paths.get(file.toURI()), StandardCharsets.ISO_8859_1)) {
			if (skip == 0) {
				modelListener.clear();
			}
			lines.skip(skip).forEach(line -> {
				if (line != null) {
					modelListener.add(processLine(line));
				}
			});
		}

	}

	private String processLine(String line) {
		// replaces tabs by spaces
		return line.replaceAll("\t", TAB_IN_SPACES);
	}

	private synchronized void loadUpdatesFromFile(int oldLength, int newLength) throws IOException {
		// int bytesToRead = newLength - oldLength;
		// special case, the file is smaller than before
		if (newLength < oldLength) {
			loadFile(0);
			return;
		}

		loadFile(modelListener.countItens());

	}

	private void createFileListener() throws IOException {
		// Stop previous file listener if exists
		stopCurrentFileListener();
		fileListener = new FileListener(file);
		fileListener.addPropertyChangeListener(this);
	}

	public String getFileName() {
		if (file != null) {
			return file.getName();
		}
		return NO_LOG_FILE;
	}

	public String getPath() {
		if (file != null) {
			return file.getPath();
		}
		return NO_LOG_FILE;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (PropertyChange.FILE_WAS_MODIFIED.name().equals(evt.getPropertyName())) {
			try {
				// System.out.println("File was modified");
				// Read only the updates
				long lastLength = (Long) evt.getOldValue();
				long newLength = (Long) evt.getNewValue();
				loadUpdatesFromFile((int) lastLength, (int) newLength);
				propertyChangeSupport.firePropertyChange(PropertyChange.LOG_FILE_CHANGED.name(), null, this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	class NullListener implements ModelListener {

		NullListener() {

		}

		@Override
		public void add(String value) {

		}

		@Override
		public void clear() {

		}

		@Override
		public void done() {

		}

		@Override
		public int countItens() {
			return 0;
		}

	}
}
