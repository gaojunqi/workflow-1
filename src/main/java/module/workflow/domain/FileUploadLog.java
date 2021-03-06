/*
 * @(#)FileUploadLog.java
 *
 * Copyright 2009 Instituto Superior Tecnico
 * Founding Authors: Luis Cruz, Paulo Abrantes
 * 
 *      https://fenix-ashes.ist.utl.pt/
 * 
 *   This file is part of the Case Handleing Based Workflow Module.
 *
 *   The Case Handleing Based Workflow Module is free software: you can
 *   redistribute it and/or modify it under the terms of the GNU Lesser General
 *   Public License as published by the Free Software Foundation, either version 
 *   3 of the License, or (at your option) any later version.
 *
 *   The Workflow Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with the Workflow Module. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package module.workflow.domain;

import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.utl.ist.fenix.tools.util.Strings;

/**
 * 
 * @author Paulo Abrantes
 * 
 */
public class FileUploadLog extends FileUploadLog_Base {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadLog.class);

    public FileUploadLog(WorkflowProcess process, User person, String... argumentsDescription) {
        super();
        init(process, person, argumentsDescription);
    }

    public static final User tryToGetUserWhoUploaded(final ProcessFile file) {
        if (file.getProcess() != null && file.getProcessWithDeleteFile() != null) {
            throw new Error(
                    "File: " + file.getExternalId() + " has an inconsistency problem. It is connected to a process twice!");
        }

        WorkflowProcess process = file.getProcess();
        if (process == null) {
            process = file.getProcessWithDeleteFile();
        }
        if (process == null) {
            throw new Error("File: " + file.getExternalId() + " has no connection with any WorkflowProcess");
        }

        final Stream<? extends WorkflowLog> logs = process.getExecutionLogStream(FileUploadLog.class);
        final FileUploadLog log = logs.map(l -> (FileUploadLog) l).filter(l -> matches(l, file)).max(WorkflowLog.COMPARATOR_BY_WHEN).orElse(null);
        return log == null ? null : log.getActivityExecutor();
    }

    /**
     * 
     * @return true if FileUploadLog has a date 1min within processFile's
     *         creation date, and the names and other details match
     */
    private static boolean matches(FileUploadLog fileUploadLog, ProcessFile processFile) {
        DateTime creationDate = processFile.getCreationDate();
        if (creationDate == null) {
            throw new IllegalArgumentException("File: " + processFile.getExternalId() + " of class: "
                    + processFile.getClass().getSimpleName() + " has no creation date");
        }

        DateTime whenOperationWasRan = fileUploadLog.getWhenOperationWasRan();

        Interval interval = null;
        if (creationDate.isBefore(whenOperationWasRan)) {
            interval = new Interval(creationDate, whenOperationWasRan);
        } else {

            interval = new Interval(whenOperationWasRan, creationDate);
        }
        if (interval.toDuration().isLongerThan(new Duration(60000))) {
            return false;
        }
        Strings descriptionArguments = fileUploadLog.getDescriptionArguments();
        if (!descriptionArguments.hasStringIgnoreCase(processFile.getFilename())) {
            return false;
        }
        if (!descriptionArguments.hasStringIgnoreCase(processFile.getDisplayName())) {
            return false;
        }

        return true;

    }

    @Override
    public String getDescription() {
        return BundleUtil.getString("resources/WorkflowResources", "label.description.FileUploadLog",
                getDescriptionArguments().toArray(new String[] {}));
    }

}
