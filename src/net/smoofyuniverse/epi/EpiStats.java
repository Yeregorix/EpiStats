package net.smoofyuniverse.epi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.app.Arguments;
import net.smoofyuniverse.common.util.KeyStoreBuilder;
import net.smoofyuniverse.common.util.ResourceUtil;
import net.smoofyuniverse.epi.stats.ObjectList;
import net.smoofyuniverse.epi.ui.UserInterface;

public class EpiStats extends Application {
	private ObjectList objectList;
	
	public static void main(String[] args) {
		new EpiStats(Arguments.parse(args));
	}
	
	public EpiStats(Arguments args) {
		super(args, "EpiStats", "1.0.0-beta3");
		initServices(Executors.newSingleThreadExecutor());
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		installKeyStore();
		
		this.objectList = new ObjectList(getWorkingDirectory().resolve("objects.olist"));
		try {
			this.objectList.read();
		} catch (IOException e) {
			getLogger().warn("Failed to read object list from file objects.olist", e);
		}
		
		Platform.runLater(() -> {
			initStage(1000, 600, true, ResourceUtil.loadImage("favicon.png"));
			setScene(new UserInterface(this)).show();
			Scene sc = getStage().getScene();
			sc.setOnKeyPressed((e) -> {
				if (e.getCode() == KeyCode.ENTER) {
					Node n = sc.getFocusOwner();
					if (n instanceof Button)
						((Button) n).fire();
				}
			});
		});
		
		checkForUpdate();
	}
	
	private void installKeyStore() {
		Path keystore = getWorkingDirectory().resolve(".keystore");
		if (!Files.exists(keystore)) {
			try {
				KeyStoreBuilder b = new KeyStoreBuilder();
				b.load();
				b.installCertificate("epicube.fr", 0);
				b.save(keystore);
			} catch (IOException | GeneralSecurityException e) {
				getLogger().error("Failed to create new keystore with certificate for epicube.fr", e);
			}
		}
		System.setProperty("javax.net.ssl.trustStore", keystore.toAbsolutePath().toString());
	}
	
	public ObjectList getObjectList() {
		return this.objectList;
	}
}
