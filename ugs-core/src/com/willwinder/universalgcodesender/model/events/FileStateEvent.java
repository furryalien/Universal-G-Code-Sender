/*
    Copyright 2021-2023 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.universalgcodesender.model.events;

import com.willwinder.universalgcodesender.model.UGSEvent;

/**
 * An event that will be dispatched when a new file has been set in
 * program.
 */
public class FileStateEvent implements UGSEvent {
    private final FileState fileState;
    private final int progressPercent;
    private final long rowCount;

    /**
     * Create a file state event
     * FILE_LOADING: This event provides a path to an unprocessed gcode file.
     * FILE_LOADING_PROGRESS: This event reports loading progress (0-100%).
     * FILE_LOADED: This event provides a path to a processed gcode file which
     * should be opened with a GcodeStreamReader.
     *
     * @param state the new file state.
     */
    public FileStateEvent(FileState state) {
        this(state, 0, 0);
    }

    /**
     * Create a file state event with progress information
     *
     * @param state the new file state
     * @param progressPercent the progress percentage (0-100) for FILE_LOADING_PROGRESS
     */
    public FileStateEvent(FileState state, int progressPercent) {
        this(state, progressPercent, 0);
    }

    /**
     * Create a file state event with progress and row count information
     *
     * @param state the new file state
     * @param progressPercent the progress percentage (0-100) for FILE_LOADING_PROGRESS
     * @param rowCount the number of rows processed so far
     */
    public FileStateEvent(FileState state, int progressPercent, long rowCount) {
        this.fileState = state;
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.rowCount = rowCount;
    }

    public FileState getFileState() {
        return fileState;
    }

    /**
     * Get the loading progress percentage (0-100)
     * Only meaningful for FILE_LOADING_PROGRESS events
     */
    public int getProgressPercent() {
        return progressPercent;
    }

    /**
     * Get the number of rows processed so far
     * Only meaningful for FILE_LOADING_PROGRESS events
     */
    public long getRowCount() {
        return rowCount;
    }
}
