/**
 * 
 */
package org.tencompetence.widgetservice.feature.wave;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.directwebremoting.ScriptBuffer;
import org.directwebremoting.ScriptSession;
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.tencompetence.widgetservice.Messages;
import org.tencompetence.widgetservice.beans.Participant;
import org.tencompetence.widgetservice.beans.SharedData;
import org.tencompetence.widgetservice.beans.WidgetInstance;
import org.tencompetence.widgetservice.manager.IWidgetAPIManager;
import org.tencompetence.widgetservice.manager.impl.WidgetAPIManager;
import org.tencompetence.widgetservice.server.LocaleHandler;

/**
 * @author scott
 *
 */
public class WaveAPIImpl implements IWaveAPI{

	/**
	 * 
	 */
	public WaveAPIImpl() {
	}
	
	

	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.feature.IFeature#getJavaScriptImpl()
	 */
	public String getJavaScriptImpl() {
		return "/wookie/dwr/interface/WaveImpl.js";
	}



	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.feature.IFeature#getJavaScriptWrapper()
	 */
	public String getJavaScriptWrapper() {
		return "/wookie/shared/js/wave.js";
	}



	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.feature.wave.IWaveAPI#getHost(java.lang.String)
	 */
	public String getHost(String idKey) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.ajaxmodel.IWidgetAPI#state(java.lang.String)
	 */
	public Map<String, String> state(String id_key) {
		HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		HttpSession session = request.getSession(true);
		Messages localizedMessages = LocaleHandler.localizeMessages(request);
		HashMap<String, String> state = new HashMap<String,String>();
		if(id_key==null){
			state.put("message", localizedMessages.getString("WidgetAPIImpl.0"));	 //$NON-NLS-1$
			return state;
		}
		IWidgetAPIManager manager = WidgetAPIManager.getManager(session, localizedMessages);
		// check if instance is valid
		WidgetInstance widgetInstance = WidgetInstance.findByIdKey(id_key);
		if (widgetInstance == null){
			state.put("message", localizedMessages.getString("WidgetAPIImpl.0"));	 //$NON-NLS-1$
			return state;			
		}
		//
		for(SharedData data : manager.getSharedDataForInstance(widgetInstance)){
			state.put(data.getDkey(), data.getDvalue());
		}
		return state;
	}
	
	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.feature.wave.IWaveAPI#getParticipants(java.lang.String)
	 */
	public String getParticipants(String id_key) {
		HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		Messages localizedMessages = LocaleHandler.localizeMessages(request);
		if(id_key == null) return localizedMessages.getString("WidgetAPIImpl.0");
		WidgetInstance widgetInstance = WidgetInstance.findByIdKey(id_key);
		if(widgetInstance==null) return localizedMessages.getString("WidgetAPIImpl.0");
		Participant[] participants = Participant.getParticipants(widgetInstance);
		String json = "{\"Participants\":[";
		String delimit = "";
		for (Participant participant: participants){
			json+=delimit+toJson(participant);
			delimit = ",";
		}
		json+="]}";
		return json;
	}
	
	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.feature.wave.IWaveAPI#getViewer(java.lang.String)
	 */
	public String getViewer(String id_key) {
		HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		Messages localizedMessages = LocaleHandler.localizeMessages(request);
		if(id_key == null) return localizedMessages.getString("WidgetAPIImpl.0");
		WidgetInstance widgetInstance = WidgetInstance.findByIdKey(id_key);
		if(widgetInstance == null) return localizedMessages.getString("WidgetAPIImpl.0");
		Participant participant = Participant.getViewer(widgetInstance);
		if (participant != null) return "{\"Participant\":"+toJson(participant)+"}"; //$NON-NLS-1$
		return null; // no viewer i.e. widget is anonymous
	}
	
	/* (non-Javadoc)
	 * @see org.tencompetence.widgetservice.feature.wave.IWaveAPI#submitDelta(java.lang.String, java.util.Map)
	 */
	public String submitDelta(String id_key, Map<String,String>map){
		HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();
		HttpSession session = request.getSession(true);
		Messages localizedMessages = LocaleHandler.localizeMessages(request);
		IWidgetAPIManager manager = WidgetAPIManager.getManager(session, localizedMessages);
		WidgetInstance widgetInstance = WidgetInstance.findByIdKey(id_key);
		if(widgetInstance == null) return localizedMessages.getString("WidgetAPIImpl.0");
		if(manager.isInstanceLocked(widgetInstance)) return localizedMessages.getString("WidgetAPIImpl.2");
		//
		for (String key: map.keySet())
		 	manager.updateSharedDataEntry(widgetInstance, key, map.get(key), false);
		notifyWidgets(widgetInstance);
		return "okay"; //$NON-NLS-1$
	}
	
	/**
	 * Converts a participant to JSON representation
	 * @param participant
	 * @return
	 */
	private String toJson(Participant participant){
		String json = "{"+
		"\"participant_id\":\""+participant.getParticipant_id()+
		"\", \"participant_display_name\":\""+participant.getParticipant_display_name()+
		"\", \"participant_thumbnail_url\":\""+participant.getParticipant_thumbnail_url()+"\"}";
		return json;
	}
	
	/**
	 * Send notifications to other widgets of shared data updates
	 */
	private void notifyWidgets(WidgetInstance widgetInstance){
		String sharedDataKey = widgetInstance.getSharedDataKey();
		String script = "Widget.onSharedUpdate(\""+sharedDataKey+"\");"; //$NON-NLS-1$ //$NON-NLS-2$
		callback(widgetInstance, script);
	}
	
	/**
	 * Sends a callback script
	 * @param widgetInstance
	 * @param call
	 */
	private void callback(WidgetInstance widgetInstance, String call){
		WebContext wctx = WebContextFactory.get();
        String currentPage = wctx.getCurrentPage();
        ScriptBuffer script = new ScriptBuffer();
        script.appendScript(call);
        // Loop over all the users on the current page
        Collection<?> pages = wctx.getScriptSessionsByPage(currentPage);
        for (Iterator<?> it = pages.iterator(); it.hasNext();){
            ScriptSession otherSession = (ScriptSession) it.next();
            otherSession.addScript(script);
        }	
	}

}