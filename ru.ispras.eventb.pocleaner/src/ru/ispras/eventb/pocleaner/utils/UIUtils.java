/**
 * Copyright (c) 2020 ISP RAS (http://www.ispras.ru)
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package ru.ispras.eventb.pocleaner.utils;

import static org.eclipse.ui.PlatformUI.getWorkbench;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rodinp.core.RodinDBException;

import ru.ispras.eventb.pocleaner.Activator;

public class UIUtils {
	/**
	 * Opens an information dialog to the user displaying the given message.
	 * 
	 * @param message The dialog message.
	 */
	public static void showInfo(final String message) {
		showInfo(null, message);
	}

	/**
	 * Opens an information dialog to the user displaying the given message.
	 * 
	 * @param title   The title of the dialog
	 * @param message The dialog message
	 */
	public static void showInfo(final String title, final String message) {
		syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(getShell(), title, message);
			}
		});

	}

	private static void syncExec(Runnable runnable) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(runnable);
	}

	static Shell getShell() {
		return getWorkbench().getModalDialogShellProvider().getShell();
	}

	public static void log(Throwable exc, String message) {
		if (exc instanceof RodinDBException) {
			final Throwable nestedExc = ((RodinDBException) exc).getException();
			if (nestedExc != null) {
				exc = nestedExc;
			}
		}
		if (message == null) {
			message = "Unknown context"; //$NON-NLS-1$
		}
		IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR, message, exc);
		log(status);
	}

	/**
	 * Logs the given status to the plug-in log.
	 */
	public static void log(IStatus status) {
		Activator.getDefault().getLog().log(status);
	}

	public static void showUnexpectedError(final Throwable exc, final String errorMessage) {
		log(exc, errorMessage);
		final IStatus status;
		if (exc instanceof CoreException) {
			IStatus s = ((CoreException) exc).getStatus();
			status = new Status(s.getSeverity(), s.getPlugin(), s.getMessage() + "\n" + errorMessage, s.getException());
		} else {
			final String msg = "Internal error " + errorMessage;
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, exc);
		}
		syncExec(new Runnable() {
			@Override
			public void run() {
				ErrorDialog.openError(getShell(), null, "Unexpected error. See log for details.", status);
			}
		});
	}
}
