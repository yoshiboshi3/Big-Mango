using UnityEngine;

public static class CoordinateMapper
{
    public static Vector2Int Map(float u, float v, int width, int height)
    {
        int x = Mathf.Clamp(Mathf.RoundToInt(u * (width - 1)), 0, width - 1);
        int y = Mathf.Clamp(Mathf.RoundToInt(v * (height - 1)), 0, height - 1);
        return new Vector2Int(x, y);
    }
}
