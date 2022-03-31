
def subdirectory_for_variant(os: str, abi: str):
    """Calculate the relative directory into which to put a paricular LiteCore variant"

    Parameters
    ----------
    os : str
        The normalized OS
    abi : str
        The normalized ABI

    Returns
    -------
    str
        The relative path name at which to store the variant
    """

    if os == "macosx":
        os = "macos"

    return f"{os}/{abi}"