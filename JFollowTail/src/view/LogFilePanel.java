package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.PatternPredicate;
import org.jdesktop.swingx.search.SearchFactory;

import interfaces.ModelListener;
import model.LogFile;
import model.PropertyChange;
import view.highlightings.Highlighting;

public class LogFilePanel extends JPanel implements PropertyChangeListener, ModelListener {

	private static final String NO_LOG_FILE = "No log file";
	private static final long serialVersionUID = 1L;
	private JXTable table;
	private JScrollPane viewLogScrollPane;
	private LogFile logFile;
	private DefaultTableModel tableModel;
	private JFollowTailFrame parentFrame;
	private boolean followTail;

	public LogFilePanel(JFollowTailFrame parentFrame) {
		this.parentFrame = parentFrame;
		createUI();
	}

	private void createUI() {
		setLayout(new BorderLayout());
		initTable();
		viewLogScrollPane = new JScrollPane(table);
		viewLogScrollPane.getViewport().setBackground(Color.WHITE);
		viewLogScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (logFile == null) {
					return;
				}
				boolean oldValue = followTail;
				JScrollBar vbar = (JScrollBar) e.getSource();
				// scroll changed by user (e.getValueIsAdjusting() == true)
				boolean adjusting = e.getValueIsAdjusting();
				if (adjusting) {
					followTail = false;
				}
				// if not follow tail and if
				// reaches the maximum of scroll -> set follow Tail
				if (!followTail
						&& e.getAdjustable().getValue() + vbar.getVisibleAmount() == e.getAdjustable().getMaximum()) {
					followTail = true;
				}

				if (oldValue != followTail) {
					firePropertyChange(PropertyChange.SCROLL_CHANGED_BY_USER.name(), oldValue, followTail);
				}
			}
		});

		add(viewLogScrollPane, BorderLayout.CENTER);
	}

	private void initTable() {
		table = new JXTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		// hide columns
		table.setTableHeader(null);
		// hide grid
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		tableModel = new DefaultTableModel(null, new String[] { "text" });
		table.setModel(tableModel);
		table.setHorizontalScrollEnabled(true);
	}

	public void processHighlightings() {
		List<Highlighting> highlightings = parentFrame.getHighlightings();
		// removes all the previous highlighters
		for (Highlighter h : table.getHighlighters()) {
			table.removeHighlighter(h);
		}

		PatternPredicate patternPredicate = null;
		ColorHighlighter highlighter = null;
		List<Highlighter> highlighters = new LinkedList<Highlighter>();
		for (Highlighting highlighting : highlightings) {
			// TODO set case sensitive and insensitive (now it's only case
			// insensitive)
			patternPredicate = new PatternPredicate(
					Pattern.compile(highlighting.getToken(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
			highlighter = new ColorHighlighter(patternPredicate, highlighting.getBackgroundColor(),
					highlighting.getForegroundColor());
			highlighters.add(highlighter);
		}
		// Reverse order to work properly
		Collections.reverse(highlighters);
		table.setHighlighters(highlighters.toArray(new Highlighter[0]));
	}

	public String getPath() {
		if (logFile != null) {
			return logFile.getPath();
		}
		return NO_LOG_FILE;
	}

	public String getFileName() {
		if (logFile != null) {
			return logFile.getFileName();
		}
		return NO_LOG_FILE;
	}

	public synchronized void setLogFile(LogFile logFile) throws IOException {
		this.logFile = logFile;
		logFile.addPropertyChangeListener(this);
		logFile.setModelListener(this);
		logFile.load();
		processHighlightings();
	}

	private void removeAllRows() {
		if (tableModel.getRowCount() > 0) {
			tableModel.setRowCount(0);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO: check if we still need this method
		if (PropertyChange.LOG_FILE_CHANGED.name().equals(evt.getPropertyName())) {
		}
	}

	public void setFollowTail(boolean followTail) {
		if (logFile == null) {
			return;
		}
		this.followTail = followTail;
		// follow Tail
		scroll(followTail);
	}

	private void scroll(boolean followTail) {
		if (followTail) {
			table.scrollRowToVisible(tableModel.getRowCount() - 1);
		}
	}

	public boolean isFollowingTail() {
		return followTail;
	}

	public void showFindDialog() {
		SearchFactory.getInstance().showFindInput(LogFilePanel.this, table.getSearchable());
	}

	public void closeLogFile() {
		logFile.stopCurrentFileListener();
	}

	@Override
	public void add(String value) {
		tableModel.addRow(new String[] { value });
		scroll(followTail);
	}

	@Override
	public void clear() {
		removeAllRows();

	}

	@Override
	public void done() {
		table.packAll();
	}

	@Override
	public int countItens() {
		return tableModel.getRowCount();
	}
}
