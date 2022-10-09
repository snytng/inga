package snytng.astah.plugin.inga;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.editor.UseCaseDiagramEditor;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
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

	JLabel  controllerLabel = new JLabel("ループ描画");
	JLabel  diagramLabel = new JLabel("");
	JLabel  ingaDiagramLabel = new JLabel("");
	JButton addButton    = new JButton(VIEW_BUNDLE.getString("Button.Add"));
	JButton deleteButton = new JButton(VIEW_BUNDLE.getString("Button.Del"));

	JLabel            selectPNStringsLabel = new JLabel("記号");
	JComboBox<String> selectPNStrings = new JComboBox<>();

	JLabel            selectPNSupplierLabel = new JLabel("リンク定義");
	JComboBox<String> selectPNSupplier = new JComboBox<>();

	JRadioButton showLoopOnlyButton = new JRadioButton("ループ要素のみ", false);
	JRadioButton showPNOnlyButton = new JRadioButton("自己強化・バランスループ要素のみ", false);

	private final String INGA_DIAGRAM_PREFIX = "inga";
	transient IUseCaseDiagram targetDiagram = null;
	transient IUseCaseDiagram ingaDiagram = null;
	transient List<IPresentation> ingaPresentationList = new ArrayList<>();
	transient List<INodePresentation> ingaCreatedNodePresentationList = new ArrayList<>();
	transient List<ILinkPresentation> ingaCreatedLinkPresentationList = new ArrayList<>();
	transient List<INodePresentation> ingaUsedNodePresentationList = new ArrayList<>();

	private Container createControllerPane() {


		// ボタンの説明追加
		showLoopOnlyButton.setToolTipText("ループに含まれる要素のみを表示");
		showPNOnlyButton.setToolTipText("自己強化・バランスの両方のループに含まれる要素のみを表示");

		// 因果ループ追加・削除ボタンの初期状態
		addButton.setEnabled(true);
		deleteButton.setEnabled(false);
		// 因果ループ追加ボタン
		addButton.addActionListener(e -> {
			try {
				IDiagram currentDiagram = diagramViewManager.getCurrentDiagram();
				if(currentDiagram instanceof IUseCaseDiagram) {
					ingaPresentationList = Arrays.asList(currentDiagram.getPresentations());

					long count = Arrays.stream(projectAccessor.getProject().getDiagrams())
							.map(IDiagram::getName)
							.filter(n -> n.startsWith(INGA_DIAGRAM_PREFIX))
							.count();
					String diagramName = INGA_DIAGRAM_PREFIX + Long.toString(count);

					TransactionManager.beginTransaction();
					ingaDiagram = usecaseDiagramEditor.createUseCaseDiagram(projectAccessor.getProject(), diagramName);
					TransactionManager.endTransaction();

					diagramLabel.setText(currentDiagram.getName());
					ingaDiagramLabel.setText(ingaDiagram.getName());
				}

			}catch(Exception ex){
				TransactionManager.abortTransaction();
				ex.printStackTrace();
			}

			createIngaNode();
			diagramViewManager.open(ingaDiagram);
			diagramViewManager.unselectAll();

			addButton.setEnabled(false);
			deleteButton.setEnabled(true);
		});

		deleteButton.addActionListener(e -> {
			targetDiagram = null;

			ingaDiagram = null;
			ingaPresentationList = new ArrayList<>();
			ingaCreatedNodePresentationList = new ArrayList<>();
			ingaCreatedLinkPresentationList = new ArrayList<>();
			ingaUsedNodePresentationList    = new ArrayList<>();

			diagramLabel.setText("");
			ingaDiagramLabel.setText("");

			addButton.setEnabled(true);
			deleteButton.setEnabled(false);
		});

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

		centerPanel.add(controllerLabel);
		centerPanel.add(diagramLabel);
		centerPanel.add(addButton);
		centerPanel.add(deleteButton);
		centerPanel.add(ingaDiagramLabel);

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

	@SuppressWarnings("unchecked")
	private void createIngaNode() {
		if(ingaDiagram == null) {
			return;
		}

		try {
			TransactionManager.beginTransaction();

			// ユースケース図を編集
			usecaseDiagramEditor.setDiagram(ingaDiagram);

			// 作成したノードを削除
			for(IPresentation p : ingaCreatedNodePresentationList) {
				usecaseDiagramEditor.deletePresentation(p);
			}

			// ノードを作成
			ingaCreatedNodePresentationList = new ArrayList<>();
			List<INodePresentation> nps = ingaPresentationList.stream()
					.filter(INodePresentation.class::isInstance)
					.map(INodePresentation.class::cast)
					.collect(Collectors.toList());

			for(INodePresentation np : nps) {
				if(np.getType().equals("UseCase") || np.getType().equals("Class")) {
					INodePresentation nnp = usecaseDiagramEditor.createNodePresentation(np.getModel(), np.getLocation());
					ingaCreatedNodePresentationList.add(nnp);

					nnp.setHeight(np.getHeight());
					nnp.setWidth(np.getWidth());
					nnp.getProperties().keySet().stream()
					.filter(k -> np.getProperty((String)k) != null)
					.forEach(k -> {
						try {
							nnp.setProperty((String)k, np.getProperty((String)k));
						} catch (InvalidEditingException e) {
							logger.log(Level.FINEST, e.getMessage(), e);
						}
					});

				} else {
					logger.log(Level.FINE, () -> "np type=" + np.getType());
				}
			}

			TransactionManager.endTransaction();
		}catch(Exception e){
			TransactionManager.abortTransaction();
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}


	@SuppressWarnings("unchecked")
	private void createIngaLink(IPresentation[] presentations) {
		if(ingaDiagram == null) {
			return;
		}

		try {
			TransactionManager.beginTransaction();

			// ユースケース図を編集
			usecaseDiagramEditor.setDiagram(ingaDiagram);

			// リンクにつながっているノードを消去
			ingaUsedNodePresentationList    = new ArrayList<>();

			// 作成したリンクを削除
			for(IPresentation p : ingaCreatedLinkPresentationList) {
				usecaseDiagramEditor.deletePresentation(p);
			}
			ingaCreatedLinkPresentationList = new ArrayList<>();

			// Presentationが存在していたら
			if (presentations != null) {
				// INodePrsentationはingaDiagramにある同じラベルのノードを追加する
				IPresentation[] ingaDiagramPresentations = ingaDiagram.getPresentations();
				Stream.of(presentations)
				.filter(INodePresentation.class::isInstance)
				.map(INodePresentation.class::cast)
				.map(node -> {
					INodePresentation ret = null;
					for(IPresentation p : ingaDiagramPresentations) {
						if(p instanceof INodePresentation) {
							if(p.getLabel() == node.getLabel()) {
								ret = (INodePresentation)p;
								break;
							}
						}
					}
					return ret;
				})
				.forEach(node -> ingaUsedNodePresentationList.add(node));

				// ILinkPresentationはリンクを追加する
				List<ILinkPresentation> lps = Stream.of(presentations)
						.filter(ILinkPresentation.class::isInstance)
						.map(ILinkPresentation.class::cast)
						.collect(Collectors.toList());

				for(ILinkPresentation lp : lps) {
					if(! linkPresentationTypes.contains(lp.getType())) {
						logger.log(Level.FINE, () -> "lp type=" + lp.getType());
						continue;
					}

					Optional<INodePresentation> source = ingaCreatedNodePresentationList.stream()
							.filter(x -> x.getLocation().equals(lp.getSource().getLocation()))
							.findFirst();
					Optional<INodePresentation> target = ingaCreatedNodePresentationList.stream()
							.filter(x -> x.getLocation().equals(lp.getTarget().getLocation()))
							.findFirst();
					if(source.isPresent() && target.isPresent()) {
						ILinkPresentation nlp = usecaseDiagramEditor.createLinkPresentation(lp.getModel(), source.get(), target.get());
						nlp.setAllPoints(lp.getAllPoints());
						nlp.setLabel(lp.getLabel());
						nlp.getProperties().keySet().stream()
						.filter(k -> lp.getProperty((String)k) != null)
						.forEach(k -> {
							try {
								nlp.setProperty((String)k, lp.getProperty((String)k));
							} catch (InvalidEditingException e) {
								logger.log(Level.FINEST, e.getMessage(), e);
							}
						});

						ingaCreatedLinkPresentationList.add(nlp);
						// リンクにつながっているノードを追加
						ingaUsedNodePresentationList.add(source.get());
						ingaUsedNodePresentationList.add(target.get());
					}
				}
			}
			TransactionManager.endTransaction();
		}catch(Exception e){
			TransactionManager.abortTransaction();
			logger.log(Level.WARNING, e.getMessage(), e);
		}
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

		// 因果分析中はtextArea表示更新せずにフォーカスする
		if(ingaDiagram != null){
			textArea.requestFocusInWindow();
			return;
		}

		try {
			// 今選択している図のタイプを取得する
			IDiagram diagram = diagramViewManager.getCurrentDiagram();

			// 今選択しているIElemnetを取得する
			List<IPresentation> selectedPresentations = Arrays.asList(diagramViewManager.getSelectedPresentations());

			// メッセージとプレゼンテーションをリセット
			messagePresentations = new ArrayList<>();

			// 選択しているユースケース図を解析して読み上げる
			if(diagram instanceof IUseCaseDiagram){
				targetDiagram = (IUseCaseDiagram)diagram;
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
		// 因果ループ作成中
		if (ingaDiagram == null) {
			if(messagePresentations != null) {
				try {
					// モデルの一時的なビューをクリア
					IDiagram currentDiagram = diagramViewManager.getCurrentDiagram();
					diagramViewManager.clearAllViewProperties(currentDiagram);

					// 選択要素がある場合
					String m = messagePresentations.get(index).message;
					Color color = Color.MAGENTA;
					if(m.startsWith(Loop.REINFORCING_NAME)) {
						color = Color.GREEN;
					} else if(m.startsWith(Loop.BALANCING_NAME)) {
						color = Color.RED;
					}

					IPresentation[] ps = messagePresentations.get(index).presentations;
					if(ps != null) {
						// 選択状態では要素選択時の更新処理を止める
						modeListSelecting = true;

						// 選択したインがループの要素を選択する
						diagramViewManager.select(ps);
						// モデルに一時的に色を付ける
						for(IPresentation p: ps) {
							diagramViewManager.setViewProperty(
									p,
									IDiagramViewManager.LINE_COLOR,
									color);
							diagramViewManager.setViewProperty(
									p,
									IDiagramViewManager.BORDER_COLOR,
									color);
						}
						// 選択状態を解除する
						modeListSelecting = false;
					}
				} catch (InvalidUsingException e1) {
					e1.printStackTrace();
				}

			}
		}
		// 因果ループ解析結果表示中
		else {
			String m = messagePresentations.get(index).message;
			Color color = Color.MAGENTA;
			if(m.startsWith(Loop.REINFORCING_NAME)) {
				color = Color.GREEN;
			} else if(m.startsWith(Loop.BALANCING_NAME)) {
				color = Color.RED;
			}

			IPresentation[] ps = messagePresentations.get(index).presentations;
			createIngaLink(ps);

			diagramViewManager.open(ingaDiagram);
			diagramViewManager.unselectAll();
			try {
				// モデルの一時的なビューをクリア
				diagramViewManager.clearAllViewProperties(ingaDiagram);
				// モデルに一時的に色を付ける
				for(IPresentation p: ingaCreatedLinkPresentationList) {
					diagramViewManager.setViewProperty(
							p,
							IDiagramViewManager.LINE_COLOR,
							color);
				}
				for(IPresentation p: ingaUsedNodePresentationList) {
					diagramViewManager.setViewProperty(
							p,
							IDiagramViewManager.BORDER_COLOR,
							color);
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
