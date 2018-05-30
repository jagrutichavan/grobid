
match = 2.0
matchU = 3.0 #weightage for Uppercase match
mismatch = -2.0
gap = -1.0


def _calc_score(matrix, x, y, seq1, seq2):
    '''Calculate score for a given x, y position in the scoring matrix.

    The score is based on the up, left, and upper-left neighbors.
    '''
    if seq1[x - 1] == seq2[y - 1]:
        if seq1[x - 1].isupper():
            #give weightage to
            similarity = matchU
        else:
            similarity = match
    else:
        similarity = mismatch
    # similarity = match if seq1[x - 1] == seq2[y - 1] and seq1[x-1].isupper() else mismatch
    diag_score = matrix[x - 1][y - 1] + similarity
    up_score = matrix[x - 1][y] + gap
    left_score = matrix[x][y - 1] + gap
    # print "diag_score, up_score, left_score)-->>>",diag_score, up_score, left_score
    return max(0, diag_score, up_score, left_score)


def create_score_matrix(seq1, seq2):
    '''Create a matrix of scores representing trial alignments of the two sequences.

    Sequence alignment can be treated as a graph search problem. This function
    creates a graph (2D matrix) of scores, which are based on trial alignments
    of different base pairs. The path with the highest cummulative score is the
    best alignment.
    '''
    rows, cols = len(seq1) + 1, len(seq2) + 1
    score_matrix = [[0 for col in range(cols)] for row in range(rows)]

    # Fill the scoring matrix.
    max_score = 0
    max_pos = None  # The row and column of the highest score in matrix.
    for i in range(1, rows):
        for j in range(1, cols):
            score = _calc_score(score_matrix, i, j, seq1, seq2)
            if score > max_score:
                max_score = score
                max_pos = (i, j)

            score_matrix[i][j] = score

    assert max_pos is not None, 'the x, y position with the highest score was not found'
    return score_matrix, max_pos, max_score


def get_start_alignment_id(score_matrix, start_pos):
    '''Find the optimal path through the matrix.
    and return the matching starting index
    This function traces a path from the bottom-right to the top-left corner of
    the scoring matrix. Each move corresponds to a match, mismatch, or gap in one
    or both of the sequences being aligned. Moves are determined by the score of
    three adjacent squares: the upper square, the left square, and the diagonal
    upper-left square.

    WHAT EACH MOVE REPRESENTS
        diagonal: match/mismatch
        up:       gap in sequence 1
        left:     gap in sequence 2
    '''

    END, DIAG, UP, LEFT = range(4)
    x, y = start_pos
    move = _next_move(score_matrix, x, y)
    while move != END:
        if move == DIAG:
            x -= 1
            y -= 1
        elif move == UP:
            x -= 1
        else:
            y -= 1

        move = _next_move(score_matrix, x, y)

    return x-1


def _next_move(score_matrix, x, y):
    diag = score_matrix[x - 1][y - 1]
    up = score_matrix[x - 1][y]
    left = score_matrix[x][y - 1]
    if diag >= up and diag >= left:     # Tie goes to the DIAG move.
        return 1 if diag != 0 else 0    # 1 signals a DIAG move. 0 signals the end.
    elif up > diag and up >= left:      # Tie goes to UP move.
        return 2 if up != 0 else 0      # UP move or end.
    elif left > diag and left > up:
        return 3 if left != 0 else 0    # LEFT move or end.
    else:
        # Execution should not reach here.
        raise ValueError('invalid move during traceback')

if __name__ == "__main__":
    seq1 = "ATAGACGACATACAGACAGCATACAGACAGCATACAGA"
    seq2 = "TTTAGCATGCGCATATCAGCAATACAGACAGATACG"
    score_matrix, start_pos, score = create_score_matrix(seq1, seq2)
    align_start = get_start_alignment_id(score_matrix, start_pos)
    print score

