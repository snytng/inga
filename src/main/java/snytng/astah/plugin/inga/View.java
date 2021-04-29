package snytng.astah.plugin.inga;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.editor.TransactionManager;
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
		//diagramViewManager.addDiagramEditorSelectionListener(this);
		diagramViewManager.addEntitySelectionListener(this);
		//projectViewManager.addEntitySelectionListener(this);
		//projectAccessor.addProjectEventListener(this);
	}

	private void removeListeners(){
		//diagramViewManager.removeDiagramEditorSelectionListener(this);
		diagramViewManager.removeEntitySelectionListener(this);	
		//projectViewManager.removeEntitySelectionListener(this);
		//projectAccessor.removeProjectEventListener(this);
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

	IUseCaseDiagram ingaDiagram = null;
	List<IPresentation> ingaPresentationList = new ArrayList<>();
	List<INodePresentation> ingaCreatedPresentationList = new ArrayList<>();

	private Container createControllerPane() {
		
		// ボタンの初期状態
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
							.filter(n -> n.startsWith("inga"))
							.count();
					String diagramName = "inga" + Long.toString(count);

					TransactionManager.beginTransaction();
					UseCaseDiagramEditor ude = projectAccessor.getDiagramEditorFactory().getUseCaseDiagramEditor();
					ingaDiagram = ude.createUseCaseDiagram(projectAccessor.getProject(), diagramName);
					TransactionManager.endTransaction();

					diagramLabel.setText(currentDiagram.getName());
					ingaDiagramLabel.setText(ingaDiagram.getName());
				}

			}catch(Exception ex){
				TransactionManager.abortTransaction();
				ex.printStackTrace();
			}
			
			diagramViewManager.open(ingaDiagram);
			diagramViewManager.unselectAll();
			
			addButton.setEnabled(false);
			deleteButton.setEnabled(true);
		});

		deleteButton.addActionListener(e -> {
			ingaDiagram = null;
			ingaPresentationList = new ArrayList<>();
			ingaCreatedPresentationList = new ArrayList<>();
			diagramLabel.setText("");
			ingaDiagramLabel.setText("");
			
			addButton.setEnabled(true);
			deleteButton.setEnabled(false);
		});

		JPanel panel = new JPanel();
		panel.add(controllerLabel);
		panel.add(diagramLabel);
		panel.add(addButton);
		panel.add(deleteButton);
		panel.add(ingaDiagramLabel);

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

	private void showInga(IPresentation[] links) {
		if(ingaDiagram == null) {
			return;
		}

		List<INodePresentation> nps = ingaPresentationList.stream()
				.filter(INodePresentation.class::isInstance)
				.map(INodePresentation.class::cast)
				.collect(Collectors.toList());

		List<ILinkPresentation> lps = Stream.of(links)
				.map(ILinkPresentation.class::cast)
				.collect(Collectors.toList());

		try {
			TransactionManager.beginTransaction();

			// ユースケース図を編集
			UseCaseDiagramEditor ude = projectAccessor.getDiagramEditorFactory().getUseCaseDiagramEditor();
			ude.setDiagram(ingaDiagram);

			// 全Presentation削除
			for(IPresentation p : ingaCreatedPresentationList) {
				ude.deletePresentation(p);
			}

			ingaCreatedPresentationList = new ArrayList<>();
			for(INodePresentation np : nps) {
				INodePresentation nnp = null;
				if(np.getType().equals("UseCase")) {
					nnp = ude.createNodePresentation(np.getModel(), np.getLocation());
					ingaCreatedPresentationList.add(nnp);
				} else {
					System.out.println("np type=" + np.getType());
				}
			}

			for(ILinkPresentation lp : lps) {
				Optional<INodePresentation> source = ingaCreatedPresentationList.stream()
						.filter(x -> x.getLocation().equals(lp.getSource().getLocation()))
						.findFirst();
				Optional<INodePresentation> target = ingaCreatedPresentationList.stream()
						.filter(x -> x.getLocation().equals(lp.getTarget().getLocation()))
						.findFirst();
				if(source.isPresent() && target.isPresent()) {
					if(linkPresentationTypes.contains(lp.getType())) {
						ILinkPresentation nlp = ude.createLinkPresentation(lp.getModel(), source.get(), target.get());
					} else {
						System.out.println("lp type=" + lp.getType());
					}
				}
			}

			TransactionManager.endTransaction();
		}catch(Exception e){
			TransactionManager.abortTransaction();
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		diagramViewManager.open(ingaDiagram);
		diagramViewManager.unselectAll();
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
		updateDiagramView();
	}

	// 読み上げ結果
	private transient MessagePresentation messagePresentation = null;

	/**
	 * 表示を更新する
	 */
	private void updateDiagramView(){

		// 因果分析中は更新をスキップ
		if(ingaDiagram != null){
			return;
		}

		try {
			// 今選択している図のタイプを取得する
			IDiagram diagram = diagramViewManager.getCurrentDiagram();

			// メッセージとプレゼンテーションをリセット
			messagePresentation = null;

			// 選択しているユースケース図を解析して読み上げる		
			if(diagram instanceof IUseCaseDiagram){
				usecaseDiagramEditor.setDiagram(diagram);
				messagePresentation = UseCaseDiagramReader.getMessagePresentation((IUseCaseDiagram)diagram, diagramViewManager, usecaseDiagramEditor);
			}
			// それ以外はなにもしない
			else {
				// no action
			}

			// メッセージのリスト化
			textArea.setListData(messagePresentation.getMessagesArray());

		}catch(Exception ex){
			logger.log(Level.WARNING, ex.getMessage());
		}
	}

	IDiagram selectedDiagram = null;
	IPresentation[] selectedPresentations = null;

	// IPluginExtraTabView
	@Override
	public void addSelectionListener(ISelectionListener listener) {
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

		if(index < 0){ // 選択項目がない場合は-1なので処理しない
			return;
		}

		if(messagePresentation.presentations != null){
			try {
				IPresentation[] sps = messagePresentation.presentations.get(index); 
				if(sps != null){
					showInga(sps);
				}
			}catch(Exception ex){
				TransactionManager.abortTransaction();
				logger.log(Level.WARNING, ex.getMessage());
			}
		}

	}

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
