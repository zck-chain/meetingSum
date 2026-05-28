package com.meetingsum.pipeline;

import com.meetingsum.pipeline.TranscriberService.WhisperResult.Segment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Diarizer {

    public List<Segment> assignSpeakers(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return segments;
        }

        List<Segment> result = new ArrayList<>(segments.size());
        boolean useSpeakerA = true;

        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            String speaker;

            if (i == 0) {
                speaker = "Speaker_A";
            } else {
                double gap = seg.start() - segments.get(i - 1).end();
                if (gap > 2.0) {
                    useSpeakerA = !useSpeakerA;
                }
                speaker = useSpeakerA ? "Speaker_A" : "Speaker_B";
            }

            result.add(new Segment(seg.start(), seg.end(), seg.text(), speaker));
        }

        return result;
    }
}
