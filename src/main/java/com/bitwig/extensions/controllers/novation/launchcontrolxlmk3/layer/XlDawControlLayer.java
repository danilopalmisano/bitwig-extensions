package com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.layer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extensions.controllers.novation.commonsmk3.RgbState;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.CcConstValues;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchControlXlHwElements;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.LaunchViewControl;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.bindings.SegmentDisplayBinding;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.control.LaunchButton;
import com.bitwig.extensions.controllers.novation.launchcontrolxlmk3.display.DisplayControl;
import com.bitwig.extensions.framework.Layer;
import com.bitwig.extensions.framework.Layers;
import com.bitwig.extensions.framework.di.Component;
import com.bitwig.extensions.framework.values.LayoutType;

@Component(tag = "XLModel")
public class XlDawControlLayer extends AbstractDawControlLayer {

    private final Layer specLauncherLayer;

    public XlDawControlLayer(final Layers layers, final ControllerHost host, final LaunchControlXlHwElements hwElements,
        final LaunchViewControl viewControl, final DisplayControl displayControl,
        final TransportHandler transportHandler) {
        super(layers, hwElements, viewControl, displayControl, transportHandler, host);
        this.specLauncherLayer = new Layer(layers, "SPEC_LAUNCHER");
        deviceRemotes.bind(this, hwElements, displayControl);

        transportHandler.bindControl(this, hwElements, 2);
        transportHandler.bindArrangerLayoutControl(this, hwElements, 2);
        transportHandler.bindLauncherLayoutControl(specLauncherLayer, hwElements, 2);
        transportHandler.getPanelLayout().addValueObserver(this::handlePanelLayoutUpdate);
        transportHandler.setTrackNavigation(this::navigateTracks);

        selectTrackBinding =
            new SegmentDisplayBinding("Select Track", cursorTrack.name(), displayControl.getTemporaryDisplay());
        this.addBinding(selectTrackBinding);

        bindNavigation(hwElements);
    }

    private void bindNavigation(final LaunchControlXlHwElements hwElements) {
        final LaunchButton trackLeftButton = hwElements.getButtons(CcConstValues.TRACK_LEFT);
        final LaunchButton trackRightButton = hwElements.getButtons(CcConstValues.TRACK_RIGHT);
        final LaunchButton pageUpButton = hwElements.getButtons(CcConstValues.PAGE_UP);
        final LaunchButton pageDownButton = hwElements.getButtons(CcConstValues.PAGE_DOWN);

        cursorDevice.hasPrevious().markInterested();
        cursorDevice.hasNext().markInterested();

        // TRACK_LEFT button - VST nav in ARRANGER, channel nav in LAUNCHER
        trackLeftButton.bindLight(this, () -> {
            if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                return cursorDevice.hasPrevious().get() ? RgbState.WHITE : RgbState.OFF;
            } else {
                return transportHandler.canNavLeft(cursorTrack) ? RgbState.WHITE : RgbState.OFF;
            }
        });
        trackLeftButton.bindRepeatHold(this, () -> {
            if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                cursorDevice.selectPrevious();
            } else {
                transportHandler.navLeft();
            }
        });

        // TRACK_RIGHT button - VST nav in ARRANGER, channel nav in LAUNCHER
        trackRightButton.bindLight(this, () -> {
            if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                return cursorDevice.hasNext().get() ? RgbState.WHITE : RgbState.OFF;
            } else {
                return transportHandler.canNavRight(cursorTrack) ? RgbState.WHITE : RgbState.OFF;
            }
        });
        trackRightButton.bindRepeatHold(this, () -> {
            if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                cursorDevice.selectNext();
            } else {
                transportHandler.navRight();
            }
        });

        // PAGE_UP button
        pageUpButton.bindLight(this, () -> {
            if (shiftState.get()) {
                return deviceRemotes.canGoBack() ? RgbState.WHITE : RgbState.OFF;
            } else if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                return transportHandler.canNavLeft(cursorTrack) ? RgbState.WHITE : RgbState.OFF;
            } else {
                return cursorDevice.hasPrevious().get() ? RgbState.WHITE : RgbState.OFF;
            }
        });
        pageUpButton.bindRepeatHold(this, () -> {
            if (shiftState.get()) {
                deviceRemotes.selectPreviousPage();
            } else if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                transportHandler.navLeft();
            } else {
                cursorDevice.selectPrevious();
            }
        });

        // PAGE_DOWN button
        pageDownButton.bindLight(this, () -> {
            if (shiftState.get()) {
                return deviceRemotes.canGoForward() ? RgbState.WHITE : RgbState.OFF;
            } else if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                return transportHandler.canNavRight(cursorTrack) ? RgbState.WHITE : RgbState.OFF;
            } else {
                return cursorDevice.hasNext().get() ? RgbState.WHITE : RgbState.OFF;
            }
        });
        pageDownButton.bindRepeatHold(this, () -> {
            if (shiftState.get()) {
                deviceRemotes.selectNextPage();
            } else if (transportHandler.getPanelLayout().get() == LayoutType.ARRANGER) {
                transportHandler.navRight();
            } else {
                cursorDevice.selectNext();
            }
        });
    }


    protected void handlePanelLayoutUpdate(final LayoutType newValue) {
        if (isActive()) {
            specLauncherLayer.setIsActive(newValue == LayoutType.LAUNCHER);
        }
    }

    @Override
    protected void onActivate() {
        super.onActivate();
        specLauncherLayer.setIsActive(transportHandler.getPanelLayout().get() == LayoutType.LAUNCHER);
        deviceRemotes.setActive(true);
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();
        specLauncherLayer.setIsActive(false);
        deviceRemotes.setActive(false);
    }
}
