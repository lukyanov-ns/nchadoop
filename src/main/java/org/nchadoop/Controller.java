/*******************************************************************************
 * Copyright 2013 Christian Schneider
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.nchadoop;

import java.io.IOException;
import java.net.URI;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileStatus;
import org.nchadoop.fs.Directory;
import org.nchadoop.fs.HdfsScanner;
import org.nchadoop.fs.SearchRoot;
import org.nchadoop.ui.HelpPopup;
import org.nchadoop.ui.MainWindow;
import org.nchadoop.ui.ScanningPopup;

import com.googlecode.lanterna.gui.GUIScreen;
import com.googlecode.lanterna.gui.Window;
import com.googlecode.lanterna.gui.dialog.MessageBox;
import com.googlecode.lanterna.gui.dialog.DialogResult;
import com.googlecode.lanterna.gui.dialog.DialogButtons;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.input.Key.Kind;

@Data
@Slf4j
public class Controller
{
    private GUIScreen     guiScreen;
    private MainWindow    mainWindow;
    private HelpPopup     helpPopup;
    private ScanningPopup scanningPopup;
    private HdfsScanner   hdfsScanner;

    public void startScan(final URI uri, String[] globFilter)
    {
        this.mainWindow.init();

        try
        {
            final SearchRoot searchRoot = this.hdfsScanner.refresh(uri, this.scanningPopup, globFilter);

            this.mainWindow.updateSearchRoot(searchRoot);
        }
        catch (final Exception e)
        {
            this.scanningPopup.close();
            log.error("Can't finish scan.", e);
            MessageBox.showMessageBox(this.guiScreen, "Error", Utils.wrapLines("Error: " + e.getMessage(), 50));
            shutdown();
        }
    }

    public void shutdown()
    {
        this.scanningPopup.close();
        this.mainWindow.close();
        this.helpPopup.close();
        this.guiScreen.getScreen().stopScreen();
        this.hdfsScanner.close();
    }

    public boolean handleGlobalKeyPressed(final Window sender, final Key key)
    {
        if (key.getCharacter() == 'q' || key.getKind() == Kind.Escape)
        {
            DialogResult quitConfirmRes = MessageBox.showMessageBox(guiScreen, "Warning",
                                                                    "Are you sure want to quit?",
                                                                    DialogButtons.OK_CANCEL);

            if (quitConfirmRes == DialogResult.OK) shutdown();
            return true;
        }
        else if (key.getCharacter() == '?')
        {
            helpPopup.show();
            return true;
        }

        return false;
    }

    public void deleteDiretory(final Directory directory)
    {
        if (directory.isRoot())
        {
            MessageBox.showMessageBox(this.guiScreen, "Error", "Couldn't the search root.");
            return;
        }

        try
        {
            if (!this.hdfsScanner.deleteDirectory(directory))
            {
                MessageBox.showMessageBox(this.guiScreen, "Error", "Couldn't delete this.");
            }
            else
            {
                this.mainWindow.changeFolder(directory.getParent());
            }
        }
        catch (final IOException e)
        {
            MessageBox.showMessageBox(this.guiScreen, "Error", "Error: " + e.getMessage());
        }
    }

    public void deleteFile(final Directory parent, final FileStatus file)
    {
        try
        {
            if (this.hdfsScanner.deleteFile(parent, file) == false)
            {
                throw new IOException("Couldn't delete this file");
            }
            else
            {
                this.mainWindow.changeFolder(parent);
            }
        }
        catch (final IOException e)
        {
            MessageBox.showMessageBox(this.guiScreen, "Error", "Error: " + e.getMessage());
        }
    }
}
