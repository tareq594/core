/*
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transitime.monitoring;

import org.transitime.applications.Core;
import org.transitime.config.DoubleConfigValue;
import org.transitime.db.hibernate.DataDbLogger;
import org.transitime.utils.EmailSender;
import org.transitime.utils.StringUtils;

/**
 * For monitoring access to database. Examines size of the db logging queue
 * to make sure that writes are not getting backed up.
 *
 * @author SkiBu Smith
 *
 */
public class DatabaseQueueMonitor extends MonitorBase {

	DoubleConfigValue maxQueueFraction = new DoubleConfigValue(
			"transitime.monitoring.maxQueueFraction", 
			0.4, 
			"If database queue fills up by more than this 0.0 - 1.0 "
			+ "fraction then database monitoring is triggered.");
	
	private static DoubleConfigValue maxQueueFractionGap =
			new DoubleConfigValue(
					"transitime.monitoring.maxQueueFractionGap", 
					0.1, 
					"When transitioning from triggered to untriggered don't "
					+ "want to send out an e-mail right away if actually "
					+ "dithering. Therefore will only send out OK e-mail if the "
					+ "value is now below maxQueueFraction - "
					+ "maxQueueFractionGap ");
	
	/********************** Member Functions **************************/

	/**
	 * Simple constructor
	 * 
	 * @param emailSender
	 * @param agencyId
	 */
	public DatabaseQueueMonitor(EmailSender emailSender, String agencyId) {
		super(emailSender, agencyId);
	}

	/* (non-Javadoc)
	 * @see org.transitime.monitoring.MonitorBase#triggered()
	 */
	@Override
	protected boolean triggered() {
		Core core = Core.getInstance();
		if (core == null)
			return false;
		
		DataDbLogger dbLogger = core.getDbLogger();
		
		setMessage("Database queue fraction=" 
				+ StringUtils.twoDigitFormat(dbLogger.queueLevel())
				+ " while max allowed fraction=" 
				+ StringUtils.twoDigitFormat(maxQueueFraction.getValue()) 
				+ ", and items in queue=" + dbLogger.queueSize()
				+ ".",
				dbLogger.queueLevel());
		
		// Determine the threshold for triggering. If already triggered
		// then lower the threshold by maxQueueFractionGap in order
		// to prevent lots of e-mail being sent out if the value is
		// dithering around maxQueueFraction.
		double threshold = maxQueueFraction.getValue();
		if (wasTriggered())
			threshold -= maxQueueFractionGap.getValue();
		
		return dbLogger.queueLevel() > threshold; 
	}

	/* (non-Javadoc)
	 * @see org.transitime.monitoring.MonitorBase#type()
	 */
	@Override
	protected String type() {
		return "Database Queue";
	}
}
