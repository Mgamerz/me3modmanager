package com.me3tweaks.modmanager.modmaker;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.me3tweaks.modmanager.ModManager;
import com.me3tweaks.modmanager.objects.Patch;

/**
 * Patch that can be compiled and applied on the fly
 * 
 * @author mjperez
 *
 */
public class DynamicPatch {
	Patch finalPatch;
	private File outputfile;

	/**
	 * Creates a dynamic mixin object from a node in modmaker xml
	 * 
	 * @param dynamicmixinNode
	 *            node to create dynamic patch object from.
	 * @throws DOMException 
	 * @throws Base64DecodingException 
	 * @throws IOException 
	 */
	public DynamicPatch(Node dynamicmixinNode) throws DOMException, IOException {
		NamedNodeMap map = dynamicmixinNode.getAttributes();
		String hexdata = dynamicmixinNode.getTextContent();
		byte[] datatowrite = DatatypeConverter.parseHexBinary(hexdata);
		outputfile = new File(ModManager.getTempDir()+"dynamicpatch-"+UUID.randomUUID().toString()+".jsf");
		FileUtils.writeByteArrayToFile(outputfile, datatowrite);
		ModManager.debugLogger.writeMessage("Wrote temporary mixin patch: "+outputfile);
		finalPatch = new Patch();
		finalPatch.setTargetPath(map.getNamedItem("targetfile").getTextContent());
		finalPatch.setTargetModule(map.getNamedItem("targetmodule").getTextContent());
		finalPatch.setPatchPath(outputfile.getAbsolutePath());
		finalPatch.setValid(true);
		finalPatch.setFinalizer(false);
		finalPatch.setPatchName(map.getNamedItem("name").getTextContent());
		finalPatch.setPatchDescription("Automatically generated ModMaker MixIn");
		finalPatch.setPatchFolderPath("");
		finalPatch.setTargetSize(Long.parseLong(map.getNamedItem("targetsize").getTextContent()));
		finalPatch.setPatchVersion(1);
		finalPatch.setPatchCMMVer(ModManager.MODDESC_VERSION_SUPPORT);
		finalPatch.setPatchAuthor("ME3Tweaks ModMaker Dynamic MixIn");
		finalPatch.setMe3tweaksid(0);
	}

	public File getOutputfile() {
		return outputfile;
	}

	public Patch getFinalPatch() {
		return finalPatch;
	}

	public void setFinalPatch(Patch finalPatch) {
		this.finalPatch = finalPatch;
	}

}