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
package ru.ispras.eventb.pocleaner;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eventb.core.EventBPlugin;
import org.eventb.core.IPORoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.pm.IProofAttempt;
import org.eventb.core.pm.IProofComponent;
import org.eventb.core.pm.IProofManager;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import ru.ispras.eventb.pocleaner.utils.UIUtils;

public class CleanPOs implements IRunnableWithProgress {
	final IPOSequent[] pos;
	private static final IProofManager PM = EventBPlugin.getProofManager();

	public CleanPOs(IPOSequent[] pos) {
		this.pos = pos;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			RodinCore.run(new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor pm) throws CoreException {
					final SubMonitor subMonitor = SubMonitor.convert(pm, pos.length);
					subMonitor.setTaskName("Cleaning proof obligations...");
					for (IPOSequent po : pos) {
						try {
							CreateEmptyProofAttempt(po, subMonitor.newChild(1));
						} catch (RodinDBException e) {
							UIUtils.log(e, "while cleaning proof: " //$NON-NLS-1$
									+ po);
						}
					}
				}
			}, monitor);
		} catch (RodinDBException e) {
			UIUtils.log(e, "while cleaning proofs"); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}
	
	private void CreateEmptyProofAttempt(IPOSequent po, IProgressMonitor monitor) throws RodinDBException{
		if (monitor.isCanceled()) {
			return;
		}
		
		final IInternalElement root = po.getRoot();
		
		if (!(root instanceof IPORoot)) {
			return;
		}
		
		monitor.subTask(po.getRoot().getElementName() + ": " + po.getElementName());

		final IProofComponent pc = PM.getProofComponent((IPORoot) root);
		final String poName = po.getElementName();

		IProofAttempt proofAttempt = pc.createProofAttempt(poName, null, null);

		try {
			proofAttempt.commit(false, null);
			pc.save(null, false);
		} finally {
			proofAttempt.dispose();
		}
	}
}