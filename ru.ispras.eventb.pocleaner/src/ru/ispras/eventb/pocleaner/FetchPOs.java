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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IPOSequent;
import org.eventb.core.IPRRoot;
import org.eventb.core.IPSStatus;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinDBException;

import ru.ispras.eventb.pocleaner.utils.UIUtils;

public class FetchPOs implements IRunnableWithProgress {

	private final List<?> poContainers;
	private final List<IPOSequent> pos = new ArrayList<IPOSequent>();
	private boolean wasCancelled = false;

	public FetchPOs(List<?> poContainers) {
		this.poContainers = poContainers;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			final SubMonitor sm = SubMonitor.convert(monitor, "Fetching proof obligations...", poContainers.size());

			for (Object o : poContainers) {
				if (o instanceof IPSStatus) {
					pos.add(((IPSStatus) o).getPOSequent());
				} else if (o instanceof IEventBRoot) {
					addAllPOs((IEventBRoot) o, pos, sm.newChild(1));
				} else if (o instanceof IRodinProject) {
					final IRodinProject rodinProject = (IRodinProject) o;
					if (rodinProject.exists()) {
						addAllPOs(rodinProject, pos, sm.newChild(1));
					} else {
						sm.worked(1);
					}
				} else {
					sm.worked(1);
				}
				if (monitor.isCanceled()) {
					pos.clear();
					wasCancelled = true;
					return;
				}
			}
		} finally {
			monitor.done();
		}
	}

	public IPOSequent[] getPOs() {
		return pos.toArray(new IPOSequent[pos.size()]);
	}

	private static void addAllPOs(final IRodinProject project, final List<IPOSequent> result,
			IProgressMonitor monitor) {
		try {
			final IPRRoot[] prRoots = project.getRootElementsOfType(IPRRoot.ELEMENT_TYPE);
			final SubMonitor sm = SubMonitor.convert(monitor, prRoots.length);
			for (IPRRoot root : prRoots) {
				addAllPOs(root, result, sm.newChild(1));
				if (monitor.isCanceled())
					return;
			}
		} catch (RodinDBException e) {
			UIUtils.log(e, "while fetching proof files in project: " //$NON-NLS-1$
					+ project);
		} finally {
			monitor.done();
		}
	}

	private static void addAllPOs(IEventBRoot root, final List<IPOSequent> result, IProgressMonitor monitor) {
		try {
			final IPOSequent[] pos = root.getPORoot().getSequents();
			result.addAll(Arrays.asList(pos));
		} catch (RodinDBException e) {
			UIUtils.log(e, "while fetching proof obligations in file: " + root); //$NON-NLS-1$
		} finally {
			monitor.done();
		}
	}

	public boolean wasCancelled() {
		return wasCancelled;
	}
}
