// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.cloud;

import biz.ganttproject.storage.StorageDialogBuilder;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.webdav.HttpDocument;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.MaskerPane;

import java.io.IOException;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavStorage implements StorageDialogBuilder.Ui {
  private final WebdavLoadService myLoadService;
  private final WebDavServerDescriptor myServer;

  public WebdavStorage(WebDavServerDescriptor cloudServer) {
    myLoadService = new WebdavLoadService(cloudServer);
    myServer = cloudServer;
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public Pane createUi(DocumentStorageUi.DocumentReceiver documentReceiver, StorageDialogBuilder.ErrorUi errorUi) {
    VBox rootPane = new VBox();
    rootPane.getStyleClass().add("pane-service-contents");
    rootPane.setPrefWidth(400);
    BreadCrumbBar<String> breadcrumbs = new BreadCrumbBar<>(BreadCrumbBar.buildTreeModel("GanttProject Cloud"));
    breadcrumbs.getStyleClass().add("title");
    rootPane.getChildren().add(breadcrumbs);

    ListView<WebDavResource> filesTable = new ListView<>();
    filesTable.setCellFactory(param -> new ListCell<WebDavResource>() {
      @Override
      protected void updateItem(WebDavResource item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
          setGraphic(null);
        } else {
          setText(item.getName());
        }
      }
    });
    filesTable.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        try {
          documentReceiver.setDocument(createDocument(filesTable.getSelectionModel().getSelectedItem()));
        } catch (IOException | Document.DocumentException e) {
          errorUi.error(e);
        }
      }
    });
    rootPane.getChildren().add(filesTable);

    StackPane stackPane = new StackPane();
    MaskerPane maskerPane = new MaskerPane();
    stackPane.getChildren().addAll(rootPane, maskerPane);
    myLoadService.setPath("/");
    myLoadService.setOnSucceeded((event) -> {
      Worker<ObservableList<WebDavResource>> source = event.getSource();
      filesTable.setItems(source.getValue());
      maskerPane.setVisible(false);
    });
    myLoadService.setOnFailed((event) -> {
      maskerPane.setVisible(false);
      errorUi.error("WebdavService failed!");
    });
    myLoadService.setOnCancelled((event) -> {
      maskerPane.setVisible(false);
      GPLogger.log("WebdavService cancelled!");
    });
    myLoadService.start();
    maskerPane.setVisible(true);
    return stackPane;
  }

  private Document createDocument(WebDavResource resource) throws IOException {
    return new HttpDocument(resource, myServer.getUsername(), myServer.getPassword(), HttpDocument.NO_LOCK);
  }
}