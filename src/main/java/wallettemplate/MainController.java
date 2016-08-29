/*
 * Copyright by the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wallettemplate;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.listeners.DownloadProgressTracker;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.MonetaryFormat;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorInitializationListener;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wallettemplate.controls.ClickableBitcoinAddress;
import wallettemplate.controls.NotificationBarPane;
import wallettemplate.utils.BitcoinUIModel;
import wallettemplate.utils.easing.EasingMode;
import wallettemplate.utils.easing.ElasticInterpolator;

import java.io.IOException;

import static wallettemplate.Main.bitcoin;

/**
 * Gets created auto-magically by FXMLLoader via reflection. The widget fields are set to the GUI controls they're named
 * after. This class handles all the updates and event handling for the main UI.
 */
public class MainController {
    final static Logger log = LoggerFactory.getLogger(MainController.class);
    public HBox controlsBox;
    public Label balance;
    public Label environment;
    public Button sendMoneyOutBtn;
    public ClickableBitcoinAddress addressControl;
    public ListView<Transaction> transactionList;
    public MonetaryFormat eightBTC = new MonetaryFormat().shift(0).minDecimals(8);
    private BitcoinUIModel model = new BitcoinUIModel();
    private NotificationBarPane.Item syncItem;

    // Called by FXMLLoader.
    public void initialize() {
        addressControl.setOpacity(0.0);
    }

    public void onBitcoinSetup() {
        model.setWallet(bitcoin.wallet());
        addressControl.addressProperty().bind(model.addressProperty());
        MonetaryFormat eightBTC = new MonetaryFormat().shift(0).minDecimals(8);
        balance.textProperty().bind(EasyBind.map(model.balanceProperty(), coin -> eightBTC.noCode().format(coin).toString()));
        environment.textProperty().bind(model.environmentProperty());
        // Don't let the user click send money when the wallet is empty.
        sendMoneyOutBtn.disableProperty().bind(model.balanceProperty().isEqualTo(Coin.ZERO));

        TorClient torClient = Main.bitcoin.peerGroup().getTorClient();
        if (torClient != null) {
            SimpleDoubleProperty torProgress = new SimpleDoubleProperty(-1);
            String torMsg = "Initialising Tor";
            syncItem = Main.instance.notificationBar.pushItem(torMsg, torProgress);
            torClient.addInitializationListener(new TorInitializationListener() {
                @Override
                public void initializationProgress(String message, int percent) {
                    Platform.runLater(() -> {
                        syncItem.label.set(torMsg + ": " + message);
                        torProgress.set(percent / 100.0);
                    });
                }

                @Override
                public void initializationCompleted() {
                    Platform.runLater(() -> {
                        syncItem.cancel();
                        showBitcoinSyncMessage();
                    });
                }
            });
        } else {
            showBitcoinSyncMessage();
        }
        model.syncProgressProperty().addListener(x -> {
            if (model.syncProgressProperty().get() >= 1.0) {
                readyToGoAnimation();
                if (syncItem != null) {
                    syncItem.cancel();
                    syncItem = null;
                }
            } else if (syncItem == null) {
                showBitcoinSyncMessage();
            }
        });
        Bindings.bindContent(transactionList.getItems(), model.getTransactions());

        transactionList.setCellFactory(param -> {
            return new TextFieldListCell<>(new StringConverter<Transaction>() {
                @Override
                public String toString(Transaction tx) {
                    Coin value = tx.getValue(Main.bitcoin.wallet());
                   if (value.isPositive()) {
                       return "Incoming payment of " + eightBTC.format(value);
                    } else { if (value.isNegative()) {
                        Address address = tx.getOutput(0).getAddressFromP2PKHScript(Main.params);
                        return "Outbound payment to " + address;
                   }

                   }
                    return "payment with id " + tx.getHash();
                }

                @Override
                public Transaction fromString(String string) {
                    return null;
                }
            });
        });
    }

    private void showBitcoinSyncMessage() {
        syncItem = Main.instance.notificationBar.pushItem("Synchronising with the Bitcoin network", model.syncProgressProperty());
    }

    public void sendMoneyOut(ActionEvent event) {
        // Hide this UI and show the send money UI. This UI won't be clickable until the user dismisses send_money.
        Main.instance.overlayUI("send_money.fxml");
    }

    public void settingsClicked(ActionEvent event) {
        Main.OverlayUI<WalletSettingsController> screen = Main.instance.overlayUI("wallet_settings.fxml");
        screen.controller.initialize(null);
    }
    public void debugClicked(ActionEvent event) {
        //String cmd = "open /Applications/Utilities/Contents/MacOS/Console.app";
        String osName = System.getProperty("os.name");
        String cmd = null;
        if (osName.startsWith("Mac")) {
             cmd = "open /Applications/Utilities/Console.app first-wallet.log";
        } else
            if (osName.startsWith("Windows")) {
               // cmd = "start notepad.exe first-wallet.log";
                cmd = "notepad.exe first-wallet.log";
            } else {
                cmd = "open vim first-wallet.log";
            };

        //                /Applications/Utilities/Console.app
        try {
            Process p = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            log.error("Failed to load log display application: " + e.getMessage());
            System.exit(1);
        }
    }
    public void restoreFromSeedAnimation() {
        // Buttons slide out ...
        TranslateTransition leave = new TranslateTransition(Duration.millis(1200), controlsBox);
        leave.setByY(80.0);
        leave.play();
    }

    public void readyToGoAnimation() {
        // Buttons slide in and clickable address appears simultaneously.
        TranslateTransition arrive = new TranslateTransition(Duration.millis(1200), controlsBox);
        arrive.setInterpolator(new ElasticInterpolator(EasingMode.EASE_OUT, 1, 2));
        arrive.setToY(0.0);
        FadeTransition reveal = new FadeTransition(Duration.millis(1200), addressControl);
        reveal.setToValue(1.0);
        ParallelTransition group = new ParallelTransition(arrive, reveal);
        group.setDelay(NotificationBarPane.ANIM_OUT_DURATION);
        group.setCycleCount(1);
        group.play();
    }

    public DownloadProgressTracker progressBarUpdater() {
        return model.getDownloadProgressTracker();
    }
}
