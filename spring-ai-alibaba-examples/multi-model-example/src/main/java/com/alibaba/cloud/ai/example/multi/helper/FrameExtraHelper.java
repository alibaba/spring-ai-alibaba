package com.alibaba.cloud.ai.example.multi.helper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import jakarta.annotation.PreDestroy;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.model.Media;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import static org.bytedeco.javacpp.Loader.deleteDirectory;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Component
public final class FrameExtraHelper implements ApplicationRunner {

	private FrameExtraHelper() {
	}

	private static final Map<String, List<String>> IMAGE_CACHE = new ConcurrentHashMap<>();

	private static final File videoUrl = new File("spring-ai-alibaba-examples/multi-model-example/src/main/resources/multimodel/video.mp4");

	private static final String framePath = "spring-ai-alibaba-examples/multi-model-example/src/main/resources/multimodel/frame/";

	private static final Logger log = LoggerFactory.getLogger(FrameExtraHelper.class);

	public static void getVideoPic() {

		List<String> strList = new ArrayList<>();
		File dir = new File(framePath);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		try (
				FFmpegFrameGrabber ff = new FFmpegFrameGrabber(videoUrl.getPath());
				Java2DFrameConverter converter = new Java2DFrameConverter()
		) {
			ff.start();
			ff.setFormat("mp4");

			int length = ff.getLengthInFrames();

			Frame frame;
			for (int i = 1; i < length; i++) {
				frame = ff.grabFrame();
				if (frame.image == null) {
					continue;
				}
				BufferedImage image = converter.getBufferedImage(frame); ;
				String path = framePath + i + ".png";
				File picFile = new File(path);
				ImageIO.write(image, "png", picFile);
				strList.add(path);
			}
			IMAGE_CACHE.put("img", strList);
			ff.stop();
		}
		catch (Exception e) {
			log.error(e.getMessage());
		}

	}

	@Override
	public void run(ApplicationArguments args) throws Exception {

		log.info("Starting to extract video frames");

		getVideoPic();

		log.info("Extracting video frames is complete");

	}

	@PreDestroy
	public void destroy() {

		try {
			deleteDirectory(new File(framePath));
		}
		catch (IOException e) {
			log.error(e.getMessage());
		}

		log.info("Delete temporary files...");
	}

	public static List<String> getFrameList() {

		assert IMAGE_CACHE.get("img") != null;
		return IMAGE_CACHE.get("img");
	}

	public static List<Media> createMediaList(int numberOfImages) {

		List<String> imgList = IMAGE_CACHE.get("img");

		int totalFrames = imgList.size();
		int interval = Math.max(totalFrames / numberOfImages, 1);

		return IntStream.range(0, numberOfImages)
				.mapToObj(i -> imgList.get(i * interval))
				.map(image -> new Media(
						MimeType.valueOf("image/png"),
						new PathResource(image)
				))
				.collect(Collectors.toList());
	}

}
