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
		ListSelectionListener {
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
	private static final String VIEW_PROPERTIES = View.class.getPackage().getName() + ".view";

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

	public View() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
			diagramViewManager = projectAccessor.getViewManager().getDiagramViewManager();
			projectViewManager = projectAccessor.getViewManager().getProjectViewManager();
		} catch (ClassNotFoundException | InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage());
		}

		initProperties();

		initComponents();

		initSettings();
	}

	private void initProperties() {
		try {
			title = VIEW_BUNDLE.getString("pluginExtraTabView.title");
			description = VIEW_BUNDLE.getString("pluginExtraTabView.description");
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage());
		}
	}

	private void initComponents() {
		// レイアウトの設定
		setLayout(new BorderLayout());
		add(createControllerPane(), BorderLayout.NORTH);
		add(createLabelPane(), BorderLayout.CENTER);
	}

	private void addListeners() {
		diagramViewManager.addDiagramEditorSelectionListener(this);
		diagramViewManager.addEntitySelectionListener(this);
		projectViewManager.addEntitySelectionListener(this);
		projectAccessor.addProjectEventListener(this);
	}

	private void removeListeners() {
		diagramViewManager.removeDiagramEditorSelectionListener(this);
		diagramViewManager.removeEntitySelectionListener(this);
		projectViewManager.removeEntitySelectionListener(this);
		projectAccessor.removeProjectEventListener(this);
	}

	JList<String> textArea = null;
	JScrollPane scrollPane = null;

	private Container createLabelPane() {

		textArea = new JList<>(new String[] {});
		textArea.addListSelectionListener(this);

		scrollPane = new JScrollPane(textArea);
		return scrollPane;
	}

	JRadioButton colorizeButton = new JRadioButton("リンク彩色", false);

	JLabel selectPNStringsLabel = new JLabel("リンク記号");
	JComboBox<String> selectPNStrings = new JComboBox<>();

	JLabel selectPNSupplierLabel = new JLabel("リンク定義");
	JComboBox<String> selectPNSupplier = new JComboBox<>();

	JRadioButton showLoopOnlyButton = new JRadioButton("ループ要素のみ", false);
	JRadioButton showPNOnlyButton = new JRadioButton("自己強化・バランスループ要素のみ", false);

	private Container createControllerPane() {
		// ボタンの説明追加
		colorizeButton.setToolTipText("オンにすると、因果ループ作成中にリンクに色付けします");
		selectPNStringsLabel.setToolTipText("因果リンクの増減を示す記号の表示文字列を選択します");
		selectPNSupplierLabel.setToolTipText("関連線をどのような因果リンクと判断するかを選択します");
		showLoopOnlyButton.setToolTipText("オンにすると、ループに含まれる要素のみ表示する");
		showPNOnlyButton.setToolTipText("オンにすると、自己強化・バランスの両方のループに含まれる要素のみ表示します");

		colorizeButton.addChangeListener(e -> {
			updateDiagramView();
			propUtil.saveSetting("colorizeButton", colorizeButton);
		});

		String[] comboPNStrings = Stream.of(Inga.PNStrings)
				.map(pn -> String.join("", pn))
				.toArray(String[]::new);
		selectPNStrings = new JComboBox<>(comboPNStrings);
		selectPNStrings.addItemListener(e -> {
			Inga.setPNStringIndex(selectPNStrings.getSelectedIndex());
			updateDiagramView();
			propUtil.saveSetting("selectPNStrings", selectPNStrings);
		});

		String[] comboSupplier = Stream.of(UseCaseDiagramReader.IngaSuppliers)
				.map(pn -> String.join("", pn))
				.toArray(String[]::new);
		selectPNSupplier = new JComboBox<>(comboSupplier);
		selectPNSupplier.addItemListener(e -> {
			UseCaseDiagramReader.setIngaSupplierIndex(selectPNSupplier.getSelectedIndex());
			updateDiagramView();
			propUtil.saveSetting("selectPNSupplier", selectPNSupplier);
		});

		showLoopOnlyButton.addChangeListener(e -> {
			updateDiagramView();
			propUtil.saveSetting("showLoopOnlyButton", showLoopOnlyButton);
		});
		showPNOnlyButton.addChangeListener(e -> {
			updateDiagramView();
			propUtil.saveSetting("showPNOnlyButton", showPNOnlyButton);
		});

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		JPanel centerPanel = new JPanel();
		JPanel eastPanel = new JPanel();
		panel.add(centerPanel, BorderLayout.CENTER);
		panel.add(eastPanel, BorderLayout.EAST);

		centerPanel.add(colorizeButton);

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
		if (!modeListSelecting) {
			updateDiagramView();
		}
	}

	// 読み上げ結果
	private transient List<MessagePresentation> messagePresentations = new ArrayList<>();

	/**
	 * 表示を更新する
	 */
	private void updateDiagramView() {
		try {
			// 今選択している図を取得する
			IDiagram diagram = diagramViewManager.getCurrentDiagram();

			// 図がなければ終了
			if (diagram == null) {
				return;
			}

			// モデルの一時的なビューをクリア
			diagramViewManager.clearAllViewProperties(diagram);

			// メッセージとプレゼンテーションをリセット
			messagePresentations = new ArrayList<>();

			// 選択しているユースケース図を解析して読み上げる
			if (diagram instanceof IUseCaseDiagram) {
				// 今選択しているIElemnetを取得する
				List<IPresentation> selectedPresentations = Arrays
						.asList(diagramViewManager.getSelectedPresentations());

				UseCaseDiagramReader udr = new UseCaseDiagramReader((IUseCaseDiagram) diagram);
				messagePresentations = udr
						.getMessagePresentation(
								selectedPresentations,
								showLoopOnlyButton.isSelected(),
								showPNOnlyButton.isSelected());

				// リンク彩色ラジオボタンが押されているときには彩色する
				if (colorizeButton.isSelected()) {
					for (Inga i : udr.getPositiveIngas()) {
						diagramViewManager.setViewProperty(
								i.p,
								IDiagramViewManager.LINE_COLOR,
								Color.BLUE);
					}
					for (Inga i : udr.getNegativeIngas()) {
						diagramViewManager.setViewProperty(
								i.p,
								IDiagramViewManager.LINE_COLOR,
								new Color(255, 69, 0) // OrangeRed
						);
					}
				}

				// メッセージをリストへ反映
				textArea.setListData(
						messagePresentations.stream()
								.map(mp -> mp.message)
								.toArray(String[]::new));
			}
			// それ以外はなにもしない
			else {
				// no action
			}

		} catch (Exception ex) {
			logger.log(Level.WARNING, ex.getMessage());
			ex.printStackTrace();
		}
	}

	/**
	 * 表示を更新する
	 */
	private void clearDiagramViewProperty() {
		try {
			// 今選択している図を取得する
			IDiagram diagram = diagramViewManager.getCurrentDiagram();

			// 図がなければ終了
			if (diagram == null) {
				return;
			}

			// モデルの一時的なビューをクリア
			diagramViewManager.clearAllViewProperties(diagram);

		} catch (Exception ex) {
			logger.log(Level.WARNING, ex.getMessage());
			ex.printStackTrace();
		}
	}

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
		clearDiagramViewProperty();
	}

	// ListSelectionListener
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) { // 操作中
			return;
		}

		int index = textArea.getSelectedIndex();
		logger.log(Level.FINE, () -> "textArea selected index=" + index);

		if (index < 0) { // 選択項目がない場合（indexは-1になる）は処理しない
			return;
		}

		// 選択項目のPresentationを因果として表示する
		if (messagePresentations != null) {
			try {
				// モデルの一時的なビューをクリア
				IDiagram currentDiagram = diagramViewManager.getCurrentDiagram();
				diagramViewManager.clearAllViewProperties(currentDiagram);

				// 選択要素がある場合
				IPresentation[] ps = messagePresentations.get(index).presentations;
				if (ps != null) {

					// 選択状態では要素選択時の更新処理を止める
					modeListSelecting = true;

					// 彩色オプションが有効なら要素に色を付ける
					if (colorizeButton.isSelected()) {
						// メッセージの内容によって一時的に変更する色を設定する
						String m = messagePresentations.get(index).message;
						Color color = Color.MAGENTA;
						if (m.startsWith(Loop.REINFORCING_NAME)) {
							color = Color.GREEN;
						} else if (m.startsWith(Loop.BALANCING_NAME)) {
							color = Color.RED;
						} else if (m.startsWith(Inga.getPositiveString())) {
							color = Color.BLUE;
						} else if (m.startsWith(Inga.getNegativeString())) {
							color = new Color(255, 69, 0); // OrangeRed
						}

						// モデルに含まれる要素を一時的に消す（グレーにする）
						Color baseColor = Color.LIGHT_GRAY;
						for (IPresentation p : currentDiagram.getPresentations()) {
							if (p instanceof ILinkPresentation) {
								diagramViewManager.setViewProperty(
										p,
										IDiagramViewManager.LINE_COLOR,
										baseColor);
								IPresentation nps = ((ILinkPresentation) p).getSourceEnd();
								diagramViewManager.setViewProperty(
										nps,
										IDiagramViewManager.BORDER_COLOR,
										baseColor);
								IPresentation npt = ((ILinkPresentation) p).getTargetEnd();
								diagramViewManager.setViewProperty(
										npt,
										IDiagramViewManager.BORDER_COLOR,
										baseColor);
							}
						}

						// ループの要素に一時的に色を付ける
						for (IPresentation p : ps) {
							if (p instanceof ILinkPresentation) {
								diagramViewManager.setViewProperty(
										p,
										IDiagramViewManager.LINE_COLOR,
										color);
								IPresentation nps = ((ILinkPresentation) p).getSourceEnd();
								diagramViewManager.setViewProperty(
										nps,
										IDiagramViewManager.BORDER_COLOR,
										color);
								IPresentation npt = ((ILinkPresentation) p).getTargetEnd();
								diagramViewManager.setViewProperty(
										npt,
										IDiagramViewManager.BORDER_COLOR,
										color);
							}
							if (p instanceof INodePresentation) {
								diagramViewManager.setViewProperty(
										p,
										IDiagramViewManager.BORDER_COLOR,
										color);
							}
						}
					}
					// 彩色オプションがオフなら選択する
					else {
						// 選択した因果ループの要素を選択する
						diagramViewManager.select(ps);
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

	// 設定の保存・読込
	private PropertiesUtil propUtil = new PropertiesUtil(".astah-inga.properties");

	private void initSettings() {
		propUtil.readPropertiesFromFile();

		propUtil.readSetting("colorizeButton", colorizeButton);
		propUtil.readSetting("selectPNStrings", selectPNStrings);
		propUtil.readSetting("selectPNSupplier", selectPNSupplier);
		propUtil.readSetting("showLoopOnlyButton", showLoopOnlyButton);
		propUtil.readSetting("showPNOnlyButton", showPNOnlyButton);
	}

}
