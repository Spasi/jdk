import org.openjdk.jmh.annotations.*;

import java.lang.foreign.*;
import java.util.concurrent.*;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 20, time = 2)
@Measurement(iterations = 3, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class FFMStructAccessTest {

    private static final int ITERS = 1000;

    private static final GroupLayout LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT_UNALIGNED.withName("x"),
        ValueLayout.JAVA_INT_UNALIGNED.withName("y"),
        ValueLayout.JAVA_INT_UNALIGNED.withName("z"),
        ValueLayout.JAVA_INT_UNALIGNED.withName("w")
    ).withName("vec4i");

    public record Vec4iAddress(long address) {
        private MemorySegment asSegment() {
            return MemorySegment.ofAddress(address).reinterpret(LAYOUT.byteSize());
        }

        int x() { return asSegment().get(ValueLayout.JAVA_INT_UNALIGNED, 0L); }
        int y() { return asSegment().get(ValueLayout.JAVA_INT_UNALIGNED, 4L); }
        int z() { return asSegment().get(ValueLayout.JAVA_INT_UNALIGNED, 8L); }
        int w() { return asSegment().get(ValueLayout.JAVA_INT_UNALIGNED, 12L); }

        Vec4iAddress x(int value) {
            asSegment().set(ValueLayout.JAVA_INT_UNALIGNED, 0L, value);
            return this;
        }
        Vec4iAddress y(int value) {
            asSegment().set(ValueLayout.JAVA_INT_UNALIGNED, 4L, value);
            return this;
        }
        Vec4iAddress z(int value) {
            asSegment().set(ValueLayout.JAVA_INT_UNALIGNED, 8L, value);
            return this;
        }
        Vec4iAddress w(int value) {
            asSegment().set(ValueLayout.JAVA_INT_UNALIGNED, 12L, value);
            return this;
        }
    }

    private MemorySegment src = Arena.global().allocate(LAYOUT);
    private MemorySegment dst = Arena.global().allocate(LAYOUT);

    private Vec4iAddress sa = new Vec4iAddress(src.address());
    private Vec4iAddress da = new Vec4iAddress(dst.address());

    // ------------------

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:LogFile=copyReinterpretInline16.xml"})
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void copyReinterpretInline16() {
        var d = this.da.address;
        var s = this.sa.address;

        var SIZE = LAYOUT.byteSize();

        for (var i = 0; i < ITERS; i++) {
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));

            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 0L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 0L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 4L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 4L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 8L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 8L));
            MemorySegment.ofAddress(d).reinterpret(SIZE)
                .set(ValueLayout.JAVA_INT_UNALIGNED, 12L, MemorySegment.ofAddress(s).reinterpret(SIZE)
                    .get(ValueLayout.JAVA_INT_UNALIGNED, 12L));
        }
    }

}