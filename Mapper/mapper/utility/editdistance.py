
def edit_distance(s1, s2, key=hash):
    """
    Calculates the edit_distance distance and the edits between two strings
    :param s1 pattern:
    :param s2 text:
    :param key:
    :return match boundary:
    """
    D = distance_matrix(s1, s2, key)
    edits = backtrace(s1, s2, D, key)
    start, end = get_match_index(edits)
    dist = D[-1][-1]
    sim = max(len(s1), len(s2)) / (1 + dist * 1.0)
    return sim, (start, end)


def distance_matrix(s1, s2, key=hash):
    """
    Generate the cost matrix for the two strings
    :param s1 string 1:
    :param s2 string 2:
    :param key:
    :return:
    """
    D = []

    previous_row = xrange(len(s2) + 1)
    D.append(list(previous_row))

    for i, c1 in enumerate(s1):
        current_row = [i + 1]
        for j, c2 in enumerate(s2):
            insertions = previous_row[j + 1] + 1
            deletions = current_row[j] + 1
            substitutions = previous_row[j] + (key(c1) != key(c2))
            current_row.append(min(insertions, deletions, substitutions))
        previous_row = current_row

        D.append(previous_row)

    return D


def backtrace(s1, s2, D, key=hash):
    """
    Trace back through the cost matrix to generate the list of edits
    :param s1 string 1:
    :param s2 string 2:
    :param D cost matrix:
    :param key:
    :return:
    """
    i, j = len(s1), len(s2)

    edits = []

    while not (i == 0 and j == 0):
        prev_cost = D[i][j]

        neighbors = []

        if i != 0 and j != 0:
            neighbors.append(D[i - 1][j - 1])
        if i != 0:
            neighbors.append(D[i - 1][j])
        if j != 0:
            neighbors.append(D[i][j - 1])

        min_cost = min(neighbors)

        if min_cost == prev_cost:
            i, j = i - 1, j - 1
            edits.append({'type': 'match', 'i': i, 'j': j})
        elif i != 0 and j != 0 and min_cost == D[i - 1][j - 1]:
            i, j = i - 1, j - 1
            edits.append({'type': 'substitution', 'i': i, 'j': j})
        elif i != 0 and min_cost == D[i - 1][j]:
            i, j = i - 1, j
            edits.append({'type': 'deletion', 'i': i, 'j': j})
        elif j != 0 and min_cost == D[i][j - 1]:
            i, j = i, j - 1
            edits.append({'type': 'insertion', 'i': i, 'j': j})

    edits.reverse()

    return edits


def get_match_index(edits):
    """
    return start and end matching indexes from the edits
    :param edits:
    :return start and end:
    """
    matched_id = []
    for i, d in enumerate(edits):
        if d['type'] == 'match':
            matched_id.append(i)
    if len(matched_id) > 0:
        return matched_id[0], matched_id[-1]
    else:
        return None, None

if __name__ == '__main__':
    t = 'ramayana'
    p = 'man'
    print edit_distance(p,t)