using NUnit.Framework;

public class CoordinateMapperTests
{
    [TestCase(0f, 0f, 0, 0)]
    [TestCase(1f, 1f, 959, 539)]
    [TestCase(0.5f, 0.5f, 480, 270)]
    public void MapNormalizedTouch(float u, float v, int expectedX, int expectedY)
    {
        var p = CoordinateMapper.Map(u, v, 960, 540);
        Assert.AreEqual(expectedX, p.x);
        Assert.AreEqual(expectedY, p.y);
    }
}
