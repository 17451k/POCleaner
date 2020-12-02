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
package ru.ispras.eventb.pocleaner.handlers;

import static ru.ispras.eventb.pocleaner.utils.UIUtils.showInfo;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eventb.core.IPOSequent;

import ru.ispras.eventb.pocleaner.FetchPOs;
import ru.ispras.eventb.pocleaner.CleanPOs;
import ru.ispras.eventb.pocleaner.utils.UIUtils;

public class POCleanHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (!(selection instanceof IStructuredSelection)) {
			showInfo("Invalid selection");
			return null;
		}

		final FetchPOs fetchPOs = new FetchPOs(((IStructuredSelection) selection).toList());
		final Shell shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();

		if (shell == null) {
			showInfo("No UI available to run the proof obligation cleaning");
			return null;
		}

		runOp(fetchPOs, shell);

		if (fetchPOs.wasCancelled())
			return null;

		final IPOSequent[] input = fetchPOs.getPOs();

		if (input.length == 0) {
			showInfo("No proof obligations to clean inside the current selection");
			return null;
		}

		final CleanPOs clean = new CleanPOs(input);
		runOp(clean, shell);
		return null;
	}

	private static void runOp(final IRunnableWithProgress op, Shell shell) {
		try {
			new ProgressMonitorDialog(shell).run(true, true, op);
		} catch (InvocationTargetException e) {
			final Throwable cause = e.getCause();
			UIUtils.showUnexpectedError(cause, "while cleaning proof obligations");
		} catch (InterruptedException e) {
			// Propagate the interruption
			Thread.currentThread().interrupt();
		}
	}

}
