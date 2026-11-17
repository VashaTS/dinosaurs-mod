package net.vsh33.dinosaurs.item;

/**
 * Hook so the common guide-book item can open the client-only book screen
 * without the main source set depending on client classes.
 */
public final class GuideBookScreens {
	@FunctionalInterface
	public interface Opener {
		void open();
	}

	private static Opener opener = () -> {
	};

	public static void setOpener(Opener opener) {
		GuideBookScreens.opener = opener;
	}

	public static void open() {
		opener.open();
	}

	private GuideBookScreens() {
	}
}
