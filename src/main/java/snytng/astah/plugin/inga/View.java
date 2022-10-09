package snytng.astah.plugin.inga;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.UseCaseDiagramEditor;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectEvent;
import com.change_vision.jude.api.inf.project.ProjectEventListener;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionEvent;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import com.change_vision.jude.api.inf.view.IEntitySelectionEvent;
import com.change_vision.jude.api.inf.view.IEntitySelectionListener;
import com.change_vision.jude.api.inf.view.IProjectViewManager;

public class View
extends
JPanel
implements
IPluginExtraTabView,
IEntitySelectionListener,
IDiagramEditorSelectionListener,
ProjectEventListener,
ListSelectionListener
{
	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(View.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	/**
	 * プロパティファイルの配置場所
	 */
	private static final String VIEW_PROPERTIES = "snytng.astah.plugin.inga.view";

	/**
	 * リソースバンドル
	 */
	private static final ResourceBundle VIEW_BUNDLE = ResourceBundle.getBundle(VIEW_PROPERTIES, Locale.getDefault());

	private String title = "<inga>";
	private String description = "<This plugin analyzes a usecase diagram as causual loop.>";

	private static final long serialVersionUID = 1L;
	private transient ProjectAccessor projectAccessor = null;
	private transient IDiagramViewManager diagramViewManager = null;
	private transient IProjectViewManager projectViewManager = null;
	private transient UseCaseDiagramEditor usecaseDiagramEditor = null;

	public View() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
			diagramViewManager = projectAccessor.getViewManager().getDiagramViewManager();
			projectViewManager = projectAccessor.getViewManager().getProjectViewManager();
			usecaseDiagramEditor = projectAccessor.getDiagramEditorFactory().getUseCaseDiagramEditor();
		} catch (ClassNotFoundException | InvalidUsingException e){
			logger.log(Level.WARNING, e.getMessage());
		}

		initProperties();

		initComponents();
	}

	private void initProperties() {
		try {
			title       = VIEW_BUNDLE.getString("pluginExtraTabView.title");
			description = VIEW_BUNDLE.getString("pluginExtraTabView.description");
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage());
		}
	}

	private void initComponents() {
		// レイアウトの設定
		setLayout(new BorderLayout());
		add(createControllerPane(), BorderLayout.NORTH);
		add(createLabelPane(), BorderLayout.CENTER);
	}

	private void addListeners(){
		diagramViewManager.addDiagramEditorSelectionListener(this);
		diagramViewManager.addEntitySelectionListener(this);
		projectViewManager.addEntitySelectionListener(this);
		projectAccessor.addProjectEventListener(this);
	}

	private void removeListeners(){
		diagramViewManager.removeDiagramEditorSelectionListener(this);
		diagramViewManager.removeEntitySelectionListener(this);
		projectViewManager.removeEntitySelectionListener(this);
		projectAccessor.removeProjectEventListener(this);
	}

	JList<String> textArea = null;
	JScrollPane scrollPane = null;
	private Container createLabelPane() {

		textArea = new JList<>(new String[]{});
		textArea.addListSelectionListener(this);

		scrollPane = new JScrollPane(textArea);
		return scrollPane;
	}

	JLabel            selectPNStringsLabel = new JLabel("記号");
	JComboBox<String> selectPNStrings = new JComboBox<>();

	JLabel            selectPNSupplierLabel = new JLabel("リンク定義");
	JComboBox<String> selectPNSupplier = new JComboBox<>();

	JRadioButton showLoopOnlyButton = new JRadioButton("ループ要素のみ", false);
	JRadioButton showPNOnlyButton = new JRadioButton("自己強化・バランスループ要素のみ", false);

	private Container createControllerPane() {
		// ボタンの説明追加
		showLoopOnlyButton.setToolTipText("ループに含まれる要素のみを表示");
		showPNOnlyButton.setToolTipText("自己強化・バランスの両方のループに含まれる要素のみを表示");

		String[] comboPNStrings = Stream.of(Inga.PNStrings)
				.map(pn -> String.join("", pn))
				.toArray(String[]::new);
		selectPNStrings = new JComboBox<>(comboPNStrings);
		selectPNStrings.addActionListener(e -> {
			Inga.setPNStringIndex(selectPNStrings.getSelectedIndex());
			updateDiagramView();
		});

		String[] comboSupplier = Stream.of(UseCaseDiagramReader.IngaSuppliers)
				.map(pn -> String.join("", pn))
				.toArray(String[]::new);
		selectPNSupplier = new JComboBox<>(comboSupplier);
		selectPNSupplier.addActionListener(e -> {
			UseCaseDiagramReader.setIngaSupplierIndex(selectPNSupplier.getSelectedIndex());
			updateDiagramView();
		});

		showLoopOnlyButton.addChangeListener(e -> {
			updateDiagramView();
		});
		showPNOnlyButton.addChangeListener(e -> {
			updateDiagramView();
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JPanel centerPanel = new JPanel();
		JPanel eastPanel = new JPanel();
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(eastPanel, BorderLayout.EAST);

		centerPanel.add(selectPNStringsLabel);
		centerPanel.add(selectPNStrings);

		centerPanel.add(selectPNSupplierLabel);
		centerPanel.add(selectPNSupplier);

		eastPanel.add(showLoopOnlyButton);
		eastPanel.add(showPNOnlyButton);

		return panel;
	}


	static List<String> linkPresentationTypes = new ArrayList<>();
	static {
		linkPresentationTypes.add("Association");
		linkPresentationTypes.add("Generalization");
		linkPresentationTypes.add("Realization");
		linkPresentationTypes.add("Dependency");
		linkPresentationTypes.add("Usage");
	}

	/**
	 * 図の選択が変更されたら表示を更新する
	 */
	@Override
	public void diagramSelectionChanged(IDiagramEditorSelectionEvent e) {
		updateDiagramView();
	}

	/**
	 * 要素の選択が変更されたら表示を更新する
	 */
	@Override
	public void entitySelectionChanged(IEntitySelectionEvent e) {
		if(! modeListSelecting) {
			updateDiagramView();
		}
	}

	// 読み上げ結果
	private transient List<MessagePresentation> messagePresentations = new ArrayList<>();

	/**
	 * 表示を更新する
	 */
	private void updateDiagramView(){
		try {
			// 今選択している図のタイプを取得する
			IDiagram diagram = diagramViewManager.getCurrentDiagram();

			// モデルの一時的なビューをクリア
			diagramViewManager.clearAllViewProperties(diagram);

			// 今選択しているIElemnetを取得する
			List<IPresentation> selectedPresentations = Arrays.asList(diagramViewManager.getSelectedPresentations());

			// メッセージとプレゼンテーションをリセット
			messagePresentations = new ArrayList<>();

			// 選択しているユースケース図を解析して読み上げる
			if(diagram instanceof IUseCaseDiagram){
				messagePresentations = new UseCaseDiagramReader((IUseCaseDiagram)diagram)
						.getMessagePresentation(
						selectedPresentations,
						showLoopOnlyButton.isSelected(),
						showPNOnlyButton.isSelected()
						);

				// メッセージをリストへ反映
				textArea.setListData(
						messagePresentations.stream()
						.map(mp -> mp.message)
						.toArray(String[]::new)
						);
			}
			// それ以外はなにもしない
			else {
				// no action
			}

		}catch(Exception ex){
			logger.log(Level.WARNING, ex.getMessage());
			ex.printStackTrace();
		}
	}

	transient IDiagram selectedDiagram = null;

	// IPluginExtraTabView
	@Override
	public void addSelectionListener(ISelectionListener listener) {
		// no action
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public void activated() {
		updateDiagramView();
		addListeners();
	}

	@Override
	public void deactivated() {
		removeListeners();
	}

	// ListSelectionListener
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getValueIsAdjusting()){ // 操作中
			return;
		}

		int index = textArea.getSelectedIndex();
		logger.log(Level.FINE, () -> "textArea selected index=" + index);

		if(index < 0){ // 選択項目がない場合（indexは-1になる）は処理しない
			return;
		}

		// 選択項目のPresentationを因果として表示する
		if(messagePresentations != null) {
			try {
				// モデルの一時的なビューをクリア
				IDiagram currentDiagram = diagramViewManager.getCurrentDiagram();
				diagramViewManager.clearAllViewProperties(currentDiagram);

				// 選択要素がある場合
				IPresentation[] ps = messagePresentations.get(index).presentations;
				if(ps != null) {
					// メッセージの内容によって一時的に変更する色を設定する
					String m = messagePresentations.get(index).message;
					Color color = Color.MAGENTA;
					if(m.startsWith(Loop.REINFORCING_NAME)) {
						color = Color.GREEN;
					} else if(m.startsWith(Loop.BALANCING_NAME)) {
						color = Color.RED;
					}

					// 選択状態では要素選択時の更新処理を止める
					modeListSelecting = true;

					// 選択した因果ループの要素を選択する
					//diagramViewManager.select(ps);

					// モデルに含まれるILinkPresentationを一時的に消す（グレーにする）
					Color baseColor = Color.LIGHT_GRAY;
					for(IPresentation p: currentDiagram.getPresentations()) {
						if(p instanceof ILinkPresentation) {
							diagramViewManager.setViewProperty(
									p,
									IDiagramViewManager.LINE_COLOR,
									baseColor);
						}
					}

					// ループの要素に一時的に色を付ける
					for(IPresentation p: ps) {
						if(p instanceof ILinkPresentation) {
							diagramViewManager.setViewProperty(
									p,
									IDiagramViewManager.LINE_COLOR,
									color);
							IPresentation nps = ((ILinkPresentation)p).getSourceEnd();
							diagramViewManager.setViewProperty(
									nps,
									IDiagramViewManager.BORDER_COLOR,
									color);
							IPresentation npt = ((ILinkPresentation)p).getTargetEnd();
							diagramViewManager.setViewProperty(
									npt,
									IDiagramViewManager.BORDER_COLOR,
									color);
						}
						if(p instanceof INodePresentation) {
							diagramViewManager.setViewProperty(
									p,
									IDiagramViewManager.BORDER_COLOR,
									color);
						}
					}

					// 選択状態を解除する
					modeListSelecting = false;
				}
			} catch (InvalidUsingException e1) {
				e1.printStackTrace();
			}

		}
	}

	transient boolean modeListSelecting = false;


	@Override
	public void projectChanged(ProjectEvent arg0) {
		updateDiagramView();
	}

	@Override
	public void projectClosed(ProjectEvent arg0) {
		// no action
	}

	@Override
	public void projectOpened(ProjectEvent arg0) {
		// no action
	}

}
